package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

/** Simule un capteur de poids : poids = volume occupé x densité moyenne des ordures. */
@Entity
@DiscriminatorValue("POIDS")
public class CapteurPoids extends Capteur {

    protected CapteurPoids() { }

    public CapteurPoids(Bac bac) { super(bac); }

    @Override
    public TypeCapteur getType() { return TypeCapteur.POIDS; }

    @Override
    public Mesure mesurer(double facteurVitesse) {
        return nouvelleMesure(TypeMesure.POIDS, getBac().estimerPoidsKg());
    }
}
