package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

/** Simule un capteur de poids : poids = volume occupé x densité moyenne des ordures. */
@Entity
@DiscriminatorValue("POIDS")
public class CapteurPoids extends Capteur {

    static final double DENSITE_KG_PAR_LITRE = 0.15;

    protected CapteurPoids() { }

    public CapteurPoids(Bac bac) { super(bac); }

    @Override
    public Mesure mesurer(double facteurVitesse) {
        Bac bac = getBac();
        double kg = bac.getNiveauPourcent() / 100.0 * bac.getCapaciteLitres() * DENSITE_KG_PAR_LITRE;
        return nouvelleMesure(TypeMesure.POIDS, kg);
    }
}
