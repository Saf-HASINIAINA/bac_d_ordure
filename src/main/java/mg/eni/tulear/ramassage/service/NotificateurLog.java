package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Alerte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Observateur minimal : journalise les alertes (le notificateur WebSocket viendra avec la couche web). */
@Component
public class NotificateurLog implements Observateur {

    private static final Logger log = LoggerFactory.getLogger(NotificateurLog.class);

    @Override
    public void notifier(Alerte alerte) {
        log.warn("ALERTE {} : {}", alerte.getType(), alerte.getMessage());
    }
}
