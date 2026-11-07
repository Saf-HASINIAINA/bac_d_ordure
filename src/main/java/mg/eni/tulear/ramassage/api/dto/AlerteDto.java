package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;

import java.time.Instant;

public record AlerteDto(Long id, TypeAlerte type, String message, Long bacId, String bacCode,
                        Instant dateCreation, boolean traitee) {
    public static AlerteDto from(Alerte a) {
        return new AlerteDto(a.getId(), a.getType(), a.getMessage(),
                a.getBac() == null ? null : a.getBac().getId(),
                a.getBac() == null ? null : a.getBac().getCode(),
                a.getDateCreation(), a.isTraitee());
    }
}
