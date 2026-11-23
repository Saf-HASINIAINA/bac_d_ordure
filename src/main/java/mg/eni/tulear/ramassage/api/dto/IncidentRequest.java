package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Incident signalé par un chauffeur pendant sa tournée ; le bac concerné est facultatif. */
public record IncidentRequest(
        @NotBlank @Size(min = 3, max = 300) String description,
        Long bacId) { }
