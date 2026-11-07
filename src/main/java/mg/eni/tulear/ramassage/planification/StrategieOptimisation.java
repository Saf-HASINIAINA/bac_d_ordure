package mg.eni.tulear.ramassage.planification;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Camion;
import mg.eni.tulear.ramassage.domain.Position;

import java.util.List;

/** Pattern Strategy : algorithme qui décide de l'ordre de passage dans les bacs retenus. */
public interface StrategieOptimisation {
    List<Bac> calculer(List<Bac> bacs, Camion camion, Position depart);
}
