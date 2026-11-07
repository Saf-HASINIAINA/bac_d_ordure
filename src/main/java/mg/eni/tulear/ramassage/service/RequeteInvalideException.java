package mg.eni.tulear.ramassage.service;

/** Données incohérentes fournies par le client (traduit en HTTP 400). */
public class RequeteInvalideException extends RuntimeException {
    public RequeteInvalideException(String message) {
        super(message);
    }
}
