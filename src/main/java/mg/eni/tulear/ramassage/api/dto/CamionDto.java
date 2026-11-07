package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Camion;
import mg.eni.tulear.ramassage.domain.enums.StatutCamion;

public record CamionDto(Long id, String immatriculation, double capaciteKg, double chargeActuelleKg,
                        StatutCamion statut, Double latitude, Double longitude) {
    public static CamionDto from(Camion c) {
        return new CamionDto(c.getId(), c.getImmatriculation(), c.getCapaciteKg(), c.getChargeActuelleKg(),
                c.getStatut(), c.getLatitude(), c.getLongitude());
    }
}
