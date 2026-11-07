package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Bilan d'activité sur une période (jours inclus, fuseau de Madagascar). */
public record RapportDto(LocalDate debut, LocalDate fin,
                         double tonnageTotalKg, int nombreTournees, int nombreCollectes,
                         double kilometresParcourus, double tauxRemplissageMoyenALaCollecte,
                         Map<TypeAlerte, Long> alertesParType,
                         List<ParZone> parZone, List<ParJour> parJour) {

    public record ParZone(Long zoneId, String zone, double tonnageKg, int collectes, int tournees, double kilometres) { }

    public record ParJour(LocalDate jour, double tonnageKg, int collectes) { }
}
