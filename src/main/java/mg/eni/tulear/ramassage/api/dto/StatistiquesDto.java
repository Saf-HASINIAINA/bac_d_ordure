package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.enums.EtatBac;

import java.util.Map;

public record StatistiquesDto(long totalBacs, Map<EtatBac, Long> bacsParEtat, double tauxRemplissageMoyen,
                              long alertesOuvertes, long tourneesEnCours, double tonnageCollecteKg) { }
