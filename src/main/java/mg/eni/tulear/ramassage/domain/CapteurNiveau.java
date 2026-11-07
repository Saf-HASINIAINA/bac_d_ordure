package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

import java.util.concurrent.ThreadLocalRandom;

/** Simule un capteur ultrason : le bac se remplit de 0 à 2 % par mesure. */
@Entity
@DiscriminatorValue("NIVEAU")
public class CapteurNiveau extends Capteur {

    static final double REMPLISSAGE_MAX_PAR_MESURE = 2.0;

    protected CapteurNiveau() { }

    public CapteurNiveau(Bac bac) { super(bac); }

    @Override
    public TypeCapteur getType() { return TypeCapteur.NIVEAU; }

    @Override
    public Mesure mesurer(double facteurVitesse) {
        double ajout = ThreadLocalRandom.current().nextDouble(0, REMPLISSAGE_MAX_PAR_MESURE) * facteurVitesse;
        double niveau = Math.min(100, getBac().getNiveauPourcent() + ajout);
        return nouvelleMesure(TypeMesure.NIVEAU, niveau);
    }
}
