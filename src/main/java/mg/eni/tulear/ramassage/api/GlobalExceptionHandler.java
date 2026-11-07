package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.ErreurApi;
import mg.eni.tulear.ramassage.service.RegleMetierException;
import mg.eni.tulear.ramassage.service.RequeteInvalideException;
import mg.eni.tulear.ramassage.service.RessourceIntrouvableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

/** Traduit les erreurs en réponses JSON : 404 introuvable, 409 règle de gestion violée, 400 requête invalide. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ResponseEntity<ErreurApi> introuvable(RessourceIntrouvableException e) {
        return reponse(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RegleMetierException.class)
    public ResponseEntity<ErreurApi> regleMetier(RegleMetierException e) {
        return reponse(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ResponseEntity<ErreurApi> requeteInvalide(RequeteInvalideException e) {
        return reponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurApi> validation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " : " + f.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return reponse(HttpStatus.BAD_REQUEST, "Requête invalide (" + detail + ")");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurApi> parametreInvalide(MethodArgumentTypeMismatchException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Valeur invalide pour le paramètre '" + e.getName() + "'");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurApi> corpsIllisible(HttpMessageNotReadableException e) {
        return reponse(HttpStatus.BAD_REQUEST, "Corps de requête JSON illisible");
    }

    private ResponseEntity<ErreurApi> reponse(HttpStatus statut, String message) {
        return ResponseEntity.status(statut).body(ErreurApi.de(statut.value(), message));
    }
}
