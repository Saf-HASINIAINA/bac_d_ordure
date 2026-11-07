package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Lectures pour l'API : les entités sont converties en DTO dans la transaction (chargement à la demande). */
@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private final BacRepository bacRepository;
    private final ZoneRepository zoneRepository;
    private final CamionRepository camionRepository;
    private final AlerteRepository alerteRepository;
    private final TourneeRepository tourneeRepository;
    private final CollecteRepository collecteRepository;
    private final ChauffeurRepository chauffeurRepository;

    public ConsultationService(BacRepository bacRepository, ZoneRepository zoneRepository,
                               CamionRepository camionRepository, AlerteRepository alerteRepository,
                               TourneeRepository tourneeRepository, CollecteRepository collecteRepository,
                               ChauffeurRepository chauffeurRepository) {
        this.bacRepository = bacRepository;
        this.zoneRepository = zoneRepository;
        this.camionRepository = camionRepository;
        this.alerteRepository = alerteRepository;
        this.tourneeRepository = tourneeRepository;
        this.collecteRepository = collecteRepository;
        this.chauffeurRepository = chauffeurRepository;
    }

    /** @param zoneId et @param etat sont facultatifs */
    public List<BacDto> bacs(Long zoneId, EtatBac etat) {
        return bacRepository.findAllAvecZone().stream()
                .filter(b -> zoneId == null || b.getZone().getId().equals(zoneId))
                .filter(b -> etat == null || b.getEtat() == etat)
                .map(BacDto::from)
                .toList();
    }

    public BacDto bac(Long id) {
        return bacRepository.findById(id)
                .map(BacDto::from)
                .orElseThrow(() -> new RessourceIntrouvableException("Bac introuvable : " + id));
    }

    public List<ZoneDto> zones() {
        return zoneRepository.findAll().stream().map(ZoneDto::from).toList();
    }

    public List<CamionDto> camions() {
        return camionRepository.findAll().stream().map(CamionDto::from).toList();
    }

    /** Chauffeurs actifs (pour choisir qui conduit une tournée). */
    public List<ChauffeurDto> chauffeurs() {
        return chauffeurRepository.findByActifTrueOrderByNom().stream().map(ChauffeurDto::from).toList();
    }

    /** @param traitee facultatif : null = toutes, false = à traiter, true = déjà traitées */
    public List<AlerteDto> alertes(Boolean traitee) {
        return alerteRepository.findAllByOrderByDateCreationDesc().stream()
                .filter(a -> traitee == null || a.isTraitee() == traitee)
                .map(AlerteDto::from)
                .toList();
    }

    public StatistiquesDto statistiques() {
        Map<EtatBac, Long> parEtat = new EnumMap<>(EtatBac.class);
        for (EtatBac e : EtatBac.values()) parEtat.put(e, 0L);
        long total = 0;
        for (Object[] ligne : bacRepository.compterParEtat()) {
            long n = (Long) ligne[1];
            parEtat.put((EtatBac) ligne[0], n);
            total += n;
        }
        return new StatistiquesDto(total, parEtat, bacRepository.niveauMoyen(),
                alerteRepository.countByTraiteeFalse(),
                tourneeRepository.findByStatut(StatutTournee.EN_COURS).size(),
                collecteRepository.tonnageCollecteKg());
    }
}
