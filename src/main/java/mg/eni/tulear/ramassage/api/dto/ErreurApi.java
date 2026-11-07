package mg.eni.tulear.ramassage.api.dto;

import java.time.Instant;

/** Corps JSON renvoyé en cas d'erreur. */
public record ErreurApi(int statut, String message, Instant horodatage) {
    public static ErreurApi de(int statut, String message) {
        return new ErreurApi(statut, message, Instant.now());
    }
}
