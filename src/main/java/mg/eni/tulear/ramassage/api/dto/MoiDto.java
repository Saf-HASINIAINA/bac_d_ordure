package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.enums.Role;

/** L'utilisateur actuellement connecté ({@code mdpAChanger} : il doit d'abord choisir un nouveau mot de passe). */
public record MoiDto(Long id, String nom, String prenom, String email, Role role, boolean mdpAChanger) { }
