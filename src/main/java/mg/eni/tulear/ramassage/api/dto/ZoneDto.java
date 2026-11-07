package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Zone;

public record ZoneDto(Long id, String nom, int frequenceCollecteHeures, Double latitudeCentre, Double longitudeCentre) {
    public static ZoneDto from(Zone z) {
        return new ZoneDto(z.getId(), z.getNom(), z.getFrequenceCollecteHeures(), z.getLatitudeCentre(), z.getLongitudeCentre());
    }
}
