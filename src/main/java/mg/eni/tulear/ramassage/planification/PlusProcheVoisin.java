package mg.eni.tulear.ramassage.planification;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Camion;
import mg.eni.tulear.ramassage.domain.Position;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Va toujours au bac le plus proche de la position courante : réduit la distance parcourue. */
public class PlusProcheVoisin implements StrategieOptimisation {

    @Override
    public List<Bac> calculer(List<Bac> bacs, Camion camion, Position depart) {
        List<Bac> restants = new ArrayList<>(bacs);
        List<Bac> ordre = new ArrayList<>(bacs.size());
        Position courante = depart;
        while (!restants.isEmpty()) {
            Position ici = courante;
            Bac suivant = restants.stream()
                    .min(Comparator.comparingDouble(b -> ici.distanceVers(b.getPosition())))
                    .orElseThrow();
            restants.remove(suivant);
            ordre.add(suivant);
            courante = suivant.getPosition();
        }
        return ordre;
    }
}
