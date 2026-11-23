package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement de son propre mot de passe : l'ancien est redemandé pour confirmer l'identité. */
public record ChangerMotDePasseRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8, max = 72, message = "doit contenir entre 8 et 72 caractères") String nouveauMotDePasse) { }
