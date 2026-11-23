package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.*;

public record ZoneRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotNull @Min(1) @Max(720) Integer frequenceCollecteHeures,
        @DecimalMin("-90") @DecimalMax("90") Double latitudeCentre,
        @DecimalMin("-180") @DecimalMax("180") Double longitudeCentre) { }
