package mg.eni.tulear.ramassage.api.dto;

import jakarta.validation.constraints.NotNull;

public record PlanifierTourneeRequest(@NotNull Long zoneId, @NotNull Long camionId, @NotNull Long chauffeurId,
                                      Long superviseurId) { }
