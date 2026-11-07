package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.Collecte;
import mg.eni.tulear.ramassage.domain.Tournee;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;

import java.time.LocalDate;
import java.util.List;

public record TourneeDto(Long id, LocalDate date, StatutTournee statut, double distanceKm,
                         Long zoneId, String zoneNom, Long camionId, String camionImmatriculation,
                         Long chauffeurId, String chauffeurNom,
                         double poidsCollecteKg, double poidsAttenduKg, List<CollecteDto> collectes) {

    public static TourneeDto from(Tournee t) {
        double attendu = t.getCollectes().stream()
                .filter(c -> !c.isEffectuee())
                .mapToDouble(c -> c.getBac().estimerPoidsKg())
                .sum();
        return new TourneeDto(t.getId(), t.getDate(), t.getStatut(), t.getDistanceKm(),
                t.getZone().getId(), t.getZone().getNom(),
                t.getCamion().getId(), t.getCamion().getImmatriculation(),
                t.getChauffeur().getId(), t.getChauffeur().getPrenom() + " " + t.getChauffeur().getNom(),
                t.calculerPoidsTotal(), attendu,
                t.getCollectes().stream().map(CollecteDto::from).toList());
    }
}
