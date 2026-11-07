package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Collecte;

import java.time.Instant;

public record CollecteDto(Long id, int ordre, Long bacId, String bacCode, double latitude, double longitude,
                          double niveauPourcent, boolean effectuee, Double poidsCollecteKg, Instant heureCollecte) {
    public static CollecteDto from(Collecte c) {
        return new CollecteDto(c.getId(), c.getOrdre(), c.getBac().getId(), c.getBac().getCode(),
                c.getBac().getLatitude(), c.getBac().getLongitude(), c.getBac().getNiveauPourcent(),
                c.isEffectuee(), c.getPoidsCollecteKg(), c.getHeureCollecte());
    }
}
