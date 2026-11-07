package mg.eni.tulear.ramassage.simulation;

import mg.eni.tulear.ramassage.repository.MesureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Supprime périodiquement les anciennes mesures pour ne pas saturer la base. */
@Component
public class PurgeMesures {

    private static final Logger log = LoggerFactory.getLogger(PurgeMesures.class);

    private final MesureRepository mesureRepository;
    private final long retentionJours;

    public PurgeMesures(MesureRepository mesureRepository,
                        @Value("${simulation.retention-jours:2}") long retentionJours) {
        this.mesureRepository = mesureRepository;
        this.retentionJours = retentionJours;
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 6 * 3_600_000L)
    @Transactional
    public void purger() {
        int supprimees = mesureRepository.supprimerAvant(Instant.now().minus(Duration.ofDays(retentionJours)));
        if (supprimees > 0) {
            log.info("Purge : {} mesures de plus de {} jour(s) supprimées", supprimees, retentionJours);
        }
    }
}
