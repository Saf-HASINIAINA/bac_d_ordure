package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.PlanifierTourneeRequest;
import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import mg.eni.tulear.ramassage.service.TourneeFacade;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public List<TourneeDto> lister(@RequestParam(required = false) StatutTournee statut,
                                   @AuthenticationPrincipal UtilisateurDetails moi) {
        return tournees.lister(statut, moi);
    }

    @GetMapping("/{id}")
    public TourneeDto obtenir(@PathVariable Long id, @AuthenticationPrincipal UtilisateurDetails moi) {
        return tournees.obtenir(id, moi);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TourneeDto planifier(@Valid @RequestBody PlanifierTourneeRequest requete) {
        return tournees.planifier(requete);
    }

    @PostMapping("/{id}/demarrer")
    public TourneeDto demarrer(@PathVariable Long id, @AuthenticationPrincipal UtilisateurDetails moi) {
        return tournees.demarrer(id, moi);
    }

    @PostMapping("/{id}/collectes/{bacId}/confirmer")
    public TourneeDto confirmerCollecte(@PathVariable Long id, @PathVariable Long bacId,
                                        @AuthenticationPrincipal UtilisateurDetails moi) {
        return tournees.confirmerCollecte(id, bacId, moi);
    }

    @PostMapping("/{id}/terminer")
    public TourneeDto terminer(@PathVariable Long id, @AuthenticationPrincipal UtilisateurDetails moi) {
        return tournees.terminer(id, moi);
    }

    @PostMapping("/{id}/annuler")
    public TourneeDto annuler(@PathVariable Long id) {
        return tournees.annuler(id);
    }
}
