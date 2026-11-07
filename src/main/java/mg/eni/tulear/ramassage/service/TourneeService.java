package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.StatutCamion;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.planification.PlanificateurTournee;
import mg.eni.tulear.ramassage.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/** Cycle de vie d'une tournée : planifier, démarrer, confirmer les vidanges, terminer, annuler. */
@Service
public class TourneeService {

    private static final Set<StatutTournee> STATUTS_ACTIFS = Set.of(StatutTournee.PLANIFIEE, StatutTournee.EN_COURS);

    private final ZoneRepository zoneRepository;
    private final CamionRepository camionRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final BacRepository bacRepository;
    private final TourneeRepository tourneeRepository;
    private final CollecteRepository collecteRepository;
    private final AlerteRepository alerteRepository;
    private final ParametreService parametreService;
    private final ServiceAlerte serviceAlerte;
    private final PlanificateurTournee planificateur;
    private final Position depot;

    public TourneeService(ZoneRepository zoneRepository, CamionRepository camionRepository,
                          UtilisateurRepository utilisateurRepository, BacRepository bacRepository,
                          TourneeRepository tourneeRepository, CollecteRepository collecteRepository,
                          AlerteRepository alerteRepository, ParametreService parametreService,
                          ServiceAlerte serviceAlerte, PlanificateurTournee planificateur,
                          @Value("${tournee.depot-latitude:-23.3550}") double depotLatitude,
                          @Value("${tournee.depot-longitude:43.6700}") double depotLongitude) {
        this.zoneRepository = zoneRepository;
        this.camionRepository = camionRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.bacRepository = bacRepository;
        this.tourneeRepository = tourneeRepository;
        this.collecteRepository = collecteRepository;
        this.alerteRepository = alerteRepository;
        this.parametreService = parametreService;
        this.serviceAlerte = serviceAlerte;
        this.planificateur = planificateur;
        this.depot = new Position(depotLatitude, depotLongitude);
    }

    /** @param superviseurId facultatif */
    @Transactional
    public Tournee planifier(Long zoneId, Long camionId, Long chauffeurId, Long superviseurId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new RessourceIntrouvableException("Zone introuvable : " + zoneId));
        Camion camion = camionRepository.findById(camionId)
                .orElseThrow(() -> new RessourceIntrouvableException("Camion introuvable : " + camionId));
        Chauffeur chauffeur = utilisateurRepository.findById(chauffeurId)
                .filter(u -> u instanceof Chauffeur && u.isActif())
                .map(u -> (Chauffeur) u)
                .orElseThrow(() -> new RessourceIntrouvableException("Chauffeur actif introuvable : " + chauffeurId));

        // RG4 : un camion / un chauffeur = une seule tournée active
        if (camion.getStatut() != StatutCamion.DISPONIBLE
                || tourneeRepository.existsByCamionIdAndStatutIn(camionId, STATUTS_ACTIFS)) {
            throw new RegleMetierException("Le camion " + camion.getImmatriculation() + " n'est pas disponible");
        }
        if (tourneeRepository.existsByChauffeurIdAndStatutIn(chauffeurId, STATUTS_ACTIFS)) {
            throw new RegleMetierException("Le chauffeur a déjà une tournée active");
        }

        Set<Long> dejaPrevus = Set.copyOf(collecteRepository.findBacIdsEnTournee(STATUTS_ACTIFS));
        List<Bac> candidats = bacRepository.findByZoneId(zoneId).stream()
                .filter(b -> !dejaPrevus.contains(b.getId()))
                .toList();

        Tournee tournee = planificateur
                .planifier(zone, candidats, camion, chauffeur, depot, parametreService.chargerSeuils())
                .orElseThrow(() -> new RegleMetierException("Aucun bac à collecter dans la zone " + zone.getNom()));

        if (superviseurId != null) {
            utilisateurRepository.findById(superviseurId)
                    .filter(u -> u instanceof Superviseur)
                    .ifPresent(u -> tournee.setSuperviseur((Superviseur) u));
        }
        return tourneeRepository.save(tournee);
    }

    @Transactional
    public Tournee demarrer(Long tourneeId) {
        Tournee tournee = charger(tourneeId);
        try {
            tournee.demarrer();
        } catch (IllegalStateException e) {
            throw new RegleMetierException(e.getMessage());
        }
        tournee.getCamion().setStatut(StatutCamion.EN_TOURNEE);
        return tournee;
    }

    /** RG7 : pèse le contenu, charge le camion, vide le bac et clôture son alerte. */
    @Transactional
    public Collecte confirmerCollecte(Long tourneeId, Long bacId) {
        Tournee tournee = charger(tourneeId);
        if (tournee.getStatut() != StatutTournee.EN_COURS) {
            throw new RegleMetierException("La tournée n'est pas en cours");
        }
        Collecte collecte = tournee.getCollectes().stream()
                .filter(c -> c.getBac().getId().equals(bacId))
                .findFirst()
                .orElseThrow(() -> new RegleMetierException("Ce bac ne fait pas partie de la tournée"));
        if (collecte.isEffectuee()) {
            throw new RegleMetierException("Ce bac a déjà été vidé");
        }

        Bac bac = collecte.getBac();
        double poids = bac.estimerPoidsKg();
        Camion camion = tournee.getCamion();
        if (!camion.peutCharger(poids)) {
            throw new RegleMetierException("Capacité du camion dépassée (RG3)");
        }
        camion.charger(poids);
        camion.deplacerVers(bac.getLatitude(), bac.getLongitude());
        collecte.confirmer(poids);

        alerteRepository.findByBacIdAndTypeAndTraiteeFalse(bacId, TypeAlerte.BAC_PLEIN)
                .forEach(Alerte::marquerTraitee);
        serviceAlerte.alerteTraitee(bacId, TypeAlerte.BAC_PLEIN);
        return collecte;
    }

    @Transactional
    public Tournee terminer(Long tourneeId) {
        Tournee tournee = charger(tourneeId);
        try {
            tournee.terminer();
        } catch (IllegalStateException e) {
            throw new RegleMetierException(e.getMessage());
        }
        Camion camion = tournee.getCamion();
        camion.decharger();
        camion.setStatut(StatutCamion.DISPONIBLE);
        camion.deplacerVers(depot.latitude(), depot.longitude());
        return tournee;
    }

    @Transactional
    public Tournee annuler(Long tourneeId) {
        Tournee tournee = charger(tourneeId);
        try {
            tournee.annuler();
        } catch (IllegalStateException e) {
            throw new RegleMetierException(e.getMessage());
        }
        return tournee;
    }

    private Tournee charger(Long id) {
        return tourneeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée introuvable : " + id));
    }
}
