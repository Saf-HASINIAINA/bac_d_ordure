package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.enums.Role;

/** L'utilisateur actuellement connecté. */
public record MoiDto(Long id, String nom, String prenom, String email, Role role) { }
