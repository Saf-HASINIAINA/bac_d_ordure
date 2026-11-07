package mg.eni.tulear.ramassage.service;

/** Ressource demandée absente (traduit en HTTP 404). */
public class RessourceIntrouvableException extends RegleMetierException {
    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
