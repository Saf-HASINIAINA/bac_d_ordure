package mg.eni.tulear.ramassage.simulation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simule des capteurs qui tombent en panne : pendant une panne, le capteur n'envoie plus aucune mesure.
 * La surveillance (RG6) doit alors lever une alerte.
 */
@Component
public class PannesSimulees {

    private final double tauxPanne;
    private final int dureePanneTicks;
    private final Map<Long, Integer> restant = new ConcurrentHashMap<>();

    /**
     * @param tauxPanne       probabilité qu'un capteur tombe en panne à chaque tick (0 = jamais)
     * @param dureePanneTicks nombre de ticks que dure une panne
     */
    public PannesSimulees(@Value("${simulation.taux-panne:0.0005}") double tauxPanne,
                          @Value("${simulation.duree-panne-ticks:120}") int dureePanneTicks) {
        this.tauxPanne = tauxPanne;
        this.dureePanneTicks = dureePanneTicks;
    }

    /** Provoque une panne à la demande (tests, démonstration). */
    public void forcer(Long capteurId, int ticks) {
        restant.put(capteurId, ticks);
    }

    /** @return true si le capteur est en panne à ce tick (donc ne doit rien envoyer) */
    public boolean enPanne(Long capteurId) {
        Integer encore = restant.get(capteurId);
        if (encore != null) {
            if (encore <= 1) restant.remove(capteurId); else restant.put(capteurId, encore - 1);
            return true;
        }
        if (tauxPanne > 0 && ThreadLocalRandom.current().nextDouble() < tauxPanne) {
            if (dureePanneTicks > 1) restant.put(capteurId, dureePanneTicks - 1);
            return true;
        }
        return false;
    }
}
