package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;

import java.time.Instant;

public record BacDto(Long id, String code, Long zoneId, String zoneNom, double latitude, double longitude,
                     double capaciteLitres, double niveauPourcent, double poidsEstimeKg,
                     TypeDechet typeDechet, EtatBac etat, Instant dateDerniereCollecte) {
    public static BacDto from(Bac b) {
        return new BacDto(b.getId(), b.getCode(), b.getZone().getId(), b.getZone().getNom(),
                b.getLatitude(), b.getLongitude(), b.getCapaciteLitres(), b.getNiveauPourcent(),
                b.estimerPoidsKg(), b.getTypeDechet(), b.getEtat(), b.getDateDerniereCollecte());
    }
}
