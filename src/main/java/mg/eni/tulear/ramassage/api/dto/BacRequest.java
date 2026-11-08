package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;

/**
 * Installation ou modification d'un bac.
 * Le code est facultatif à l'installation (généré : BAC-001, BAC-002...) et ignoré à la modification.
 */
public record BacRequest(
        @Pattern(regexp = "^$|^[A-Za-z0-9._-]{1,50}$", message = "lettres, chiffres, point, tiret ou souligné (50 maximum)") String code,
        @NotNull Long zoneId,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitude,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitude,
        @NotNull @DecimalMin(value = "10", message = "10 litres au minimum") @DecimalMax(value = "10000", message = "10 000 litres au maximum") Double capaciteLitres,
        TypeDechet typeDechet) { }
