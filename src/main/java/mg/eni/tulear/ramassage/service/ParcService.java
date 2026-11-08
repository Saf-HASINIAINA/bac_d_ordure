package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import mg.eni.tulear.ramassage.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Gestion du parc : installation et retrait des bacs, zones et camions, mise en maintenance.
 * Les droits (administrateur ou superviseur) sont appliqués dans SecurityConfig.
 */
@Service
@Transactional
public class ParcService {

    private static final Set<StatutTournee> ACTIFS = Set.of(StatutTournee.PLANIFIEE, StatutTournee.EN_COURS);

    private final BacRepository bacRepository;
    private final CapteurRepository capteurRepository;
    private final ZoneRepository zoneRepository;
    private final CamionRepository camionRepository;
    private final TourneeRepository tourneeRepository;
    private final CollecteRepository collecteRepository;
    private final ParametreService parametreService;
    private final ServiceAlerte serviceAlerte;
    private final double depotLatitude;
    private final double depotLongitude;

    public ParcService(BacRepository bacRepository, CapteurRepository capteurRepository,
                       ZoneRepository zoneRepository, CamionRepository camionRepository,
                       TourneeRepository tourneeRepository, CollecteRepository collecteRepository,
                       ParametreService parametreService, ServiceAlerte serviceAlerte,
                       @Value("${tournee.depot-latitude:-23.3550}") double depotLatitude,
                       @Value("${tournee.depot-longitude:43.6700}") double depotLongitude) {
        this.bacRepository = bacRepository;
        this.capteurRepository = capteurRepository;
        this.zoneRepository = zoneRepository;
        this.camionRepository = camionRepository;
        this.tourneeRepository = tourneeRepository;
        this.collecteRepository = collecteRepository;
        this.parametreService = parametreService;
        this.serviceAlerte = serviceAlerte;
        this.depotLatitude = depotLatitude;
        this.depotLongitude = depotLongitude;
    }

    /* ======================= bacs ======================= */

    /** Installe un bac : vide, avec ses trois capteurs (niveau, poids, gaz) qui commencent à émettre. */
    public BacDto installerBac(BacRequest r) {
        Zone zone = zone(r.zoneId());
        String code = r.code() == null || r.code().isBlank() ? genererCodeBac() : r.code().trim().toUpperCase();
        if (bacRepository.existsByCode(code)) {
            throw new RegleMetierException("Le code " + code + " est déjà utilisé par un autre bac");
        }
        Bac bac = bacRepository.save(new Bac(code, zone, r.latitude(), r.longitude(), r.capaciteLitres(),
                r.typeDechet() != null ? r.typeDechet() : TypeDechet.MENAGER));
        for (TypeCapteur type : TypeCapteur.values()) {
            capteurRepository.save(CapteurFactory.creer(type, bac));
        }
        return BacDto.from(bac);
    }

    public BacDto modifierBac(Long id, BacRequest r) {
        Bac bac = bac(id);
        bac.modifier(zone(r.zoneId()), r.latitude(), r.longitude(), r.capaciteLitres(),
                r.typeDechet() != null ? r.typeDechet() : bac.getTypeDechet());
        return BacDto.from(bac);
    }

    /** Retire un bac qui n'a jamais été collecté ; sinon il faut le mettre hors service pour garder l'historique. */
    public void supprimerBac(Long id) {
        Bac bac = bac(id);
        if (collecteRepository.existsByBacId(id)) {
            throw new RegleMetierException("Ce bac apparaît dans des tournées : mettez-le hors service plutôt que de le supprimer");
        }
        bacRepository.delete(bac);
        for (TypeAlerte type : TypeAlerte.values()) {
            serviceAlerte.alerteTraitee(id, type); // nettoie la mémoire des alertes ouvertes
        }
    }

    public BacDto mettreBacHorsService(Long id) {
        Bac bac = bac(id);
        if (bac.getEtat() == EtatBac.HORS_SERVICE) {
            throw new RegleMetierException("Ce bac est déjà hors service");
        }
        if (collecteRepository.findBacIdsEnTournee(ACTIFS).contains(id)) {
            throw new RegleMetierException("Ce bac est prévu dans une tournée planifiée ou en cours : annulez-la ou terminez-la d'abord");
        }
        bac.mettreHorsService();
        return BacDto.from(bac);
    }

    public BacDto remettreBacEnService(Long id) {
        Bac bac = bac(id);
        if (bac.getEtat() != EtatBac.HORS_SERVICE) {
            throw new RegleMetierException("Ce bac est déjà en service");
        }
        bac.remettreEnService(parametreService.chargerSeuils());
        return BacDto.from(bac);
    }

    private String genererCodeBac() {
        long numero = bacRepository.count() + 1;
        String code;
        do {
            code = "BAC-%03d".formatted(numero++);
        } while (bacRepository.existsByCode(code));
        return code;
    }

    /* ======================= zones ======================= */

    public ZoneDto creerZone(ZoneRequest r) {
        String nom = r.nom().trim();
        if (zoneRepository.findByNomIgnoreCase(nom).isPresent()) {
            throw new RegleMetierException("Une zone nommée « " + nom + " » existe déjà");
        }
        Zone zone = new Zone(nom, r.frequenceCollecteHeures(), 0, 0);
        zone.modifier(nom, r.frequenceCollecteHeures(), r.latitudeCentre(), r.longitudeCentre());
        return ZoneDto.from(zoneRepository.save(zone), 0);
    }

    public ZoneDto modifierZone(Long id, ZoneRequest r) {
        Zone zone = zone(id);
        String nom = r.nom().trim();
        zoneRepository.findByNomIgnoreCase(nom).filter(autre -> !autre.getId().equals(id)).ifPresent(autre -> {
            throw new RegleMetierException("Une zone nommée « " + nom + " » existe déjà");
        });
        zone.modifier(nom, r.frequenceCollecteHeures(), r.latitudeCentre(), r.longitudeCentre());
        return ZoneDto.from(zone, bacRepository.countByZoneId(id));
    }

    public void supprimerZone(Long id) {
        Zone zone = zone(id);
        long bacs = bacRepository.countByZoneId(id);
        if (bacs > 0) {
            throw new RegleMetierException("Cette zone contient encore %d bac(s) : déplacez-les ou retirez-les d'abord".formatted(bacs));
        }
        if (tourneeRepository.existsByZoneId(id)) {
            throw new RegleMetierException("Cette zone a un historique de tournées et ne peut pas être supprimée");
        }
        zoneRepository.delete(zone);
    }

    /* ======================= camions ======================= */

    public CamionDto creerCamion(CamionRequest r) {
        String immatriculation = normaliser(r.immatriculation());
        if (camionRepository.findByImmatriculationIgnoreCase(immatriculation).isPresent()) {
            throw new RegleMetierException("Un camion immatriculé " + immatriculation + " existe déjà");
        }
        return CamionDto.from(camionRepository.save(
                new Camion(immatriculation, r.capaciteKg(), depotLatitude, depotLongitude)));
    }

    public CamionDto modifierCamion(Long id, CamionRequest r) {
        Camion camion = camion(id);
        String immatriculation = normaliser(r.immatriculation());
        camionRepository.findByImmatriculationIgnoreCase(immatriculation).filter(autre -> !autre.getId().equals(id))
                .ifPresent(autre -> {
                    throw new RegleMetierException("Un camion immatriculé " + immatriculation + " existe déjà");
                });
        if (r.capaciteKg() < camion.getChargeActuelleKg()) {
            throw new RegleMetierException("La capacité ne peut pas être inférieure à la charge actuelle du camion");
        }
        camion.modifier(immatriculation, r.capaciteKg());
        return CamionDto.from(camion);
    }

    public void supprimerCamion(Long id) {
        Camion camion = camion(id);
        if (tourneeRepository.existsByCamionId(id)) {
            throw new RegleMetierException("Ce camion a un historique de tournées : déclarez-le en panne plutôt que de le supprimer");
        }
        camionRepository.delete(camion);
    }

    public CamionDto declarerCamionEnPanne(Long id) {
        Camion camion = camion(id);
        if (camion.getStatut() == StatutCamion.EN_PANNE) {
            throw new RegleMetierException("Ce camion est déjà en panne");
        }
        if (camion.getStatut() == StatutCamion.EN_TOURNEE
                || tourneeRepository.existsByCamionIdAndStatutIn(id, ACTIFS)) {
            throw new RegleMetierException("Ce camion est affecté à une tournée planifiée ou en cours");
        }
        camion.setStatut(StatutCamion.EN_PANNE);
        return CamionDto.from(camion);
    }

    public CamionDto remettreCamionDisponible(Long id) {
        Camion camion = camion(id);
        if (camion.getStatut() != StatutCamion.EN_PANNE) {
            throw new RegleMetierException("Seul un camion en panne peut être remis à disposition");
        }
        camion.setStatut(StatutCamion.DISPONIBLE);
        return CamionDto.from(camion);
    }

    /* ======================= utilitaires ======================= */

    private Bac bac(Long id) {
        return bacRepository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Bac introuvable : " + id));
    }

    private Zone zone(Long id) {
        return zoneRepository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Zone introuvable : " + id));
    }

    private Camion camion(Long id) {
        return camionRepository.findById(id).orElseThrow(() -> new RessourceIntrouvableException("Camion introuvable : " + id));
    }

    private static String normaliser(String immatriculation) {
        return immatriculation.trim().toUpperCase();
    }
}
