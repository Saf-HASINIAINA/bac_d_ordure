package mg.eni.tulear.ramassage.planification;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Camion;
import mg.eni.tulear.ramassage.domain.Position;

import java.util.Comparator;
import java.util.List;

/** Passe d'abord par les bacs les plus remplis, quelle que soit la distance (RG5). */
public class PrioriteRemplissage implements StrategieOptimisation {

    @Override
    public List<Bac> calculer(List<Bac> bacs, Camion camion, Position depart) {
        return bacs.stream()
                .sorted(Comparator.comparingDouble(Bac::getNiveauPourcent).reversed())
                .toList();
    }
}
