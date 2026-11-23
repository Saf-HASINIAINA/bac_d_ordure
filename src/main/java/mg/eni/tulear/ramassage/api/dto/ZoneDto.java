package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Zone;

public record ZoneDto(Long id, String nom, int frequenceCollecteHeures, Double latitudeCentre, Double longitudeCentre,
                      long nombreBacs) {
    public static ZoneDto from(Zone z, long nombreBacs) {
        return new ZoneDto(z.getId(), z.getNom(), z.getFrequenceCollecteHeures(), z.getLatitudeCentre(),
                z.getLongitudeCentre(), nombreBacs);
    }
}
