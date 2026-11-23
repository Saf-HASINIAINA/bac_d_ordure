package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangerMotDePasseRequest(
        @NotBlank String ancienMotDePasse,
        @NotBlank @Size(min = 8, max = 72, message = "doit contenir entre 8 et 72 caractères") String nouveauMotDePasse) { }
