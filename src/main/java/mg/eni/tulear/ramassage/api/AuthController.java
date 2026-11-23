package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.ChangerMotDePasseRequest;
import mg.eni.tulear.ramassage.api.dto.InscriptionRequest;
import mg.eni.tulear.ramassage.api.dto.MessageDto;
import mg.eni.tulear.ramassage.api.dto.MoiDto;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import mg.eni.tulear.ramassage.service.UtilisateurService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** La connexion (/api/auth/connexion) et la déconnexion (/api/auth/deconnexion) sont gérées par Spring Security. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UtilisateurService utilisateurService;

    public AuthController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @PostMapping("/inscription")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageDto inscription(@Valid @RequestBody InscriptionRequest requete) {
        utilisateurService.inscrire(requete);
        return new MessageDto("Compte créé. Un administrateur doit le valider avant que vous puissiez vous connecter.");
    }

    @PostMapping("/mot-de-passe")
    public MessageDto changerMotDePasse(@Valid @RequestBody ChangerMotDePasseRequest requete,
                                        @AuthenticationPrincipal UtilisateurDetails moi) {
        utilisateurService.changerMotDePasse(moi.getId(), requete.ancienMotDePasse(), requete.nouveauMotDePasse());
        return new MessageDto("Mot de passe modifié.");
    }

    @GetMapping("/moi")
    public MoiDto moi(@AuthenticationPrincipal UtilisateurDetails moi) {
        return new MoiDto(moi.getId(), moi.getNom(), moi.getPrenom(), moi.getUsername(), moi.getRole());
    }
}
