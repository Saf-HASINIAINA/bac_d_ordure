package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.PlanifierTourneeRequest;
import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.service.TourneeFacade;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournees")
public class TourneeController {

    private final TourneeFacade tournees;

    public TourneeController(TourneeFacade tournees) {
        this.tournees = tournees;
    }

    @GetMapping
    public List<TourneeDto> lister(@RequestParam(required = false) StatutTournee statut) {
        return tournees.lister(statut);
    }

    @GetMapping("/{id}")
    public TourneeDto obtenir(@PathVariable Long id) {
        return tournees.obtenir(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TourneeDto planifier(@Valid @RequestBody PlanifierTourneeRequest requete) {
        return tournees.planifier(requete);
    }

    @PostMapping("/{id}/demarrer")
    public TourneeDto demarrer(@PathVariable Long id) {
        return tournees.demarrer(id);
    }

    @PostMapping("/{id}/collectes/{bacId}/confirmer")
    public TourneeDto confirmerCollecte(@PathVariable Long id, @PathVariable Long bacId) {
        return tournees.confirmerCollecte(id, bacId);
    }

    @PostMapping("/{id}/terminer")
    public TourneeDto terminer(@PathVariable Long id) {
        return tournees.terminer(id);
    }

    @PostMapping("/{id}/annuler")
    public TourneeDto annuler(@PathVariable Long id) {
        return tournees.annuler(id);
    }
}
