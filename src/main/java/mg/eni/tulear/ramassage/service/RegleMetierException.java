package mg.eni.tulear.ramassage.service;

/** Violation d'une règle de gestion (RG3, RG4, RG8...). */
public class RegleMetierException extends RuntimeException {
    public RegleMetierException(String message) {
        super(message);
    }
}
