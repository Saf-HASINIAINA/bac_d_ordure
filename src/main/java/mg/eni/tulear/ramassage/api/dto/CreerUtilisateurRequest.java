package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import mg.eni.tulear.ramassage.domain.enums.Role;

/** Création d'un compte par l'administrateur (actif immédiatement, rôle au choix). */
public record CreerUtilisateurRequest(
        @NotNull Role role,
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Size(max = 100) String prenom,
        @NotBlank @Email @Size(max = 200) String email,
        @NotBlank @Size(min = 8, max = 72, message = "doit contenir entre 8 et 72 caractères") String motDePasse,
        @Size(max = 50) String numeroPermis) { }
