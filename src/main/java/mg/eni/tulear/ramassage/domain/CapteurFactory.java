package mg.eni.tulear.ramassage.domain;

import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;

/** Pattern Factory : création du bon capteur selon son type. */
public final class CapteurFactory {

    private CapteurFactory() { }

    public static Capteur creer(TypeCapteur type, Bac bac) {
        return switch (type) {
            case NIVEAU -> new CapteurNiveau(bac);
            case POIDS -> new CapteurPoids(bac);
            case GAZ -> new CapteurGaz(bac);
        };
    }
}
