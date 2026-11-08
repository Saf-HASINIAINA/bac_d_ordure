package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.AlerteDto;
import mg.eni.tulear.ramassage.api.dto.IncidentRequest;
import mg.eni.tulear.ramassage.api.dto.PlanifierTourneeRequest;
import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Collecte;
import mg.eni.tulear.ramassage.domain.Tournee;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Façade de l'API autour de {@link TourneeService} : renvoie des DTO construits dans la transaction
 * et applique la règle « un chauffeur ne voit et ne manipule que ses propres tournées ».
 */
@Service
@Transactional
public class TourneeFacade {

    private final TourneeService tourneeService;
    private final TourneeRepository tourneeRepository;
    private final ServiceAlerte serviceAlerte;

    public TourneeFacade(TourneeService tourneeService, TourneeRepository tourneeRepository,
                         ServiceAlerte serviceAlerte) {
        this.tourneeService = tourneeService;
        this.tourneeRepository = tourneeRepository;
        this.serviceAlerte = serviceAlerte;
    }

    @Transactional(readOnly = true)
    public List<TourneeDto> lister(StatutTournee statut, UtilisateurDetails moi) {
        List<Tournee> tournees = statut == null
                ? tourneeRepository.findAllByOrderByIdDesc()
                : tourneeRepository.findByStatutOrderByIdDesc(statut);
        return tournees.stream()
                .filter(t -> moi.getRole() != Role.CHAUFFEUR || t.getChauffeur().getId().equals(moi.getId()))
                .map(TourneeDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TourneeDto obtenir(Long id, UtilisateurDetails moi) {
        Tournee tournee = tourneeRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée introuvable : " + id));
        verifierAcces(tournee, moi);
        return TourneeDto.from(tournee);
    }

    /** Réservé aux administrateurs et superviseurs (voir SecurityConfig). */
    public TourneeDto planifier(PlanifierTourneeRequest r) {
        return TourneeDto.from(tourneeService.planifier(r.zoneId(), r.camionId(), r.chauffeurId(), r.superviseurId()));
    }

    public TourneeDto demarrer(Long id, UtilisateurDetails moi) {
        controler(id, moi);
        return TourneeDto.from(tourneeService.demarrer(id));
    }

    public TourneeDto confirmerCollecte(Long tourneeId, Long bacId, UtilisateurDetails moi) {
        controler(tourneeId, moi);
        tourneeService.confirmerCollecte(tourneeId, bacId);
        return obtenir(tourneeId, moi);
    }

    public TourneeDto terminer(Long id, UtilisateurDetails moi) {
        controler(id, moi);
        return TourneeDto.from(tourneeService.terminer(id));
    }

    /** Un chauffeur signale un incident sur sa tournée (bac en panne, accès bloqué, camion en difficulté...). */
    public AlerteDto signalerIncident(Long tourneeId, IncidentRequest r, UtilisateurDetails moi) {
        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée introuvable : " + tourneeId));
        verifierAcces(tournee, moi);
        if (tournee.getStatut() != StatutTournee.PLANIFIEE && tournee.getStatut() != StatutTournee.EN_COURS) {
            throw new RegleMetierException("Un incident ne peut être signalé que sur une tournée planifiée ou en cours");
        }
        Bac bac = null;
        if (r.bacId() != null) {
            bac = tournee.getCollectes().stream().map(Collecte::getBac)
                    .filter(b -> b.getId().equals(r.bacId())).findFirst()
                    .orElseThrow(() -> new RegleMetierException("Ce bac ne fait pas partie de la tournée"));
        }
        String message = "Incident signalé par %s %s (tournée n°%d%s) : %s".formatted(
                moi.getPrenom(), moi.getNom(), tournee.getId(), bac != null ? ", bac " + bac.getCode() : "",
                r.description().trim());
        return AlerteDto.from(serviceAlerte.signalerIncident(bac, message));
    }

    /** Réservé aux administrateurs et superviseurs (voir SecurityConfig). */
    public TourneeDto annuler(Long id) {
        return TourneeDto.from(tourneeService.annuler(id));
    }

    private void controler(Long tourneeId, UtilisateurDetails moi) {
        if (moi.getRole() != Role.CHAUFFEUR) return;
        Tournee tournee = tourneeRepository.findById(tourneeId)
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée introuvable : " + tourneeId));
        verifierAcces(tournee, moi);
    }

    private void verifierAcces(Tournee tournee, UtilisateurDetails moi) {
        if (moi.getRole() == Role.CHAUFFEUR && !tournee.getChauffeur().getId().equals(moi.getId())) {
            throw new AccessDeniedException("Cette tournée est confiée à un autre chauffeur");
        }
    }
}
