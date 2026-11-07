package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Chauffeur;
import mg.eni.tulear.ramassage.domain.Utilisateur;
import mg.eni.tulear.ramassage.domain.enums.Role;

/** Jamais le mot de passe, même haché. */
public record UtilisateurDto(Long id, String nom, String prenom, String email, Role role, boolean actif,
                             String numeroPermis) {
    public static UtilisateurDto from(Utilisateur u) {
        return new UtilisateurDto(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(), u.getRole(), u.isActif(),
                u instanceof Chauffeur c ? c.getNumeroPermis() : null);
    }
}
