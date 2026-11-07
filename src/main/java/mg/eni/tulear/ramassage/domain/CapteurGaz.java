package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

import java.util.concurrent.ThreadLocalRandom;

/** Simule un capteur gaz/température : valeurs normales, avec un pic rare (risque d'incendie). */
@Entity
@DiscriminatorValue("GAZ")
public class CapteurGaz extends Capteur {

    static final double PROBABILITE_PIC = 0.002;

    protected CapteurGaz() { }

    public CapteurGaz(Bac bac) { super(bac); }

    @Override
    public Mesure mesurer(double facteurVitesse) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        double valeur = rnd.nextDouble() < PROBABILITE_PIC
                ? rnd.nextDouble(300, 500)
                : rnd.nextDouble(20, 80);
        return nouvelleMesure(TypeMesure.GAZ, valeur);
    }
}
