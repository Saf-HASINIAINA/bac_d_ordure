package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.repository.EntreeAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Supprime les entrées du journal d'audit plus vieilles que {@code app.audit.retention-jours} (90 par défaut),
 * pour que le journal ne fasse pas grossir la base sans fin. Une valeur de 0 ou moins désactive la purge.
 */
@Component
public class PurgeJournalAudit {

    private static final Logger log = LoggerFactory.getLogger(PurgeJournalAudit.class);

    private final EntreeAuditRepository repository;
    private final long retentionJours;
    private final Clock horloge;

    @Autowired
    public PurgeJournalAudit(EntreeAuditRepository repository,
                             @Value("${app.audit.retention-jours:90}") long retentionJours) {
        this(repository, retentionJours, Clock.systemUTC());
    }

    PurgeJournalAudit(EntreeAuditRepository repository, long retentionJours, Clock horloge) {
        this.repository = repository;
        this.retentionJours = retentionJours;
        this.horloge = horloge;
    }

    /** Une fois par jour (première passe 2 minutes après le démarrage). */
    @Scheduled(initialDelay = 120_000, fixedDelay = 24 * 3_600_000L)
    @Transactional
    public int purger() {
        if (retentionJours <= 0) return 0;
        try {
            int supprimees = repository.supprimerAvant(Instant.now(horloge).minus(Duration.ofDays(retentionJours)));
            if (supprimees > 0) {
                log.info("Purge du journal d'audit : {} entrée(s) de plus de {} jour(s) supprimée(s)", supprimees, retentionJours);
            }
            return supprimees;
        } catch (RuntimeException e) { // une purge ratée ne doit pas gêner l'application : nouvel essai demain
            log.error("Purge du journal d'audit impossible : {}", e.getMessage());
            return 0;
        }
    }
}
