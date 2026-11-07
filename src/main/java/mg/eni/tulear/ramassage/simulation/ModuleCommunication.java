package mg.eni.tulear.ramassage.simulation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/** Simule le module GSM/LoRa : une partie des messages est perdue. */
@Component
public class ModuleCommunication {

    private final double tauxPerte;

    public ModuleCommunication(@Value("${simulation.taux-perte:0.05}") double tauxPerte) {
        this.tauxPerte = tauxPerte;
    }

    /** @return true si le message est bien arrivé au serveur */
    public boolean envoyer() {
        return ThreadLocalRandom.current().nextDouble() >= tauxPerte;
    }
}
