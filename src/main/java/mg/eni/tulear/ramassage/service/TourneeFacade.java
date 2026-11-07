package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.PlanifierTourneeRequest;
import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.Tournee;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Façade de l'API autour de {@link TourneeService} : renvoie des DTO construits dans la transaction. */
@Service
@Transactional
public class TourneeFacade {

    private final TourneeService tourneeService;
    private final TourneeRepository tourneeRepository;

    public TourneeFacade(TourneeService tourneeService, TourneeRepository tourneeRepository) {
        this.tourneeService = tourneeService;
        this.tourneeRepository = tourneeRepository;
    }

    @Transactional(readOnly = true)
    public List<TourneeDto> lister(StatutTournee statut) {
        List<Tournee> tournees = statut == null
                ? tourneeRepository.findAllByOrderByIdDesc()
                : tourneeRepository.findByStatutOrderByIdDesc(statut);
        return tournees.stream().map(TourneeDto::from).toList();
    }

    @Transactional(readOnly = true)
    public TourneeDto obtenir(Long id) {
        return tourneeRepository.findById(id)
                .map(TourneeDto::from)
                .orElseThrow(() -> new RessourceIntrouvableException("Tournée introuvable : " + id));
    }

    public TourneeDto planifier(PlanifierTourneeRequest r) {
        return TourneeDto.from(tourneeService.planifier(r.zoneId(), r.camionId(), r.chauffeurId(), r.superviseurId()));
    }

    public TourneeDto demarrer(Long id) {
        return TourneeDto.from(tourneeService.demarrer(id));
    }

    public TourneeDto confirmerCollecte(Long tourneeId, Long bacId) {
        tourneeService.confirmerCollecte(tourneeId, bacId);
        return obtenir(tourneeId);
    }

    public TourneeDto terminer(Long id) {
        return TourneeDto.from(tourneeService.terminer(id));
    }

    public TourneeDto annuler(Long id) {
        return TourneeDto.from(tourneeService.annuler(id));
    }
}
