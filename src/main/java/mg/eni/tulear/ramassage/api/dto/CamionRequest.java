package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.*;

public record CamionRequest(
        @NotBlank @Size(max = 30) String immatriculation,
        @NotNull @DecimalMin(value = "100", message = "100 kg au minimum") @DecimalMax(value = "50000", message = "50 000 kg au maximum") Double capaciteKg) { }
