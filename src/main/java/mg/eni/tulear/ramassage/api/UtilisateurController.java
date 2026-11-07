package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.CreerUtilisateurRequest;
import mg.eni.tulear.ramassage.api.dto.UtilisateurDto;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import mg.eni.tulear.ramassage.service.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestion des comptes : réservée aux administrateurs (voir SecurityConfig). */
@RestController
@RequestMapping("/api/utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public List<UtilisateurDto> lister() {
        return utilisateurService.lister();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UtilisateurDto creer(@Valid @RequestBody CreerUtilisateurRequest requete) {
        return utilisateurService.creer(requete);
    }

    @PostMapping("/{id}/activer")
    public UtilisateurDto activer(@PathVariable Long id) {
        return utilisateurService.activer(id);
    }

    @PostMapping("/{id}/desactiver")
    public UtilisateurDto desactiver(@PathVariable Long id, @AuthenticationPrincipal UtilisateurDetails moi) {
        return utilisateurService.desactiver(id, moi.getId());
    }
}
