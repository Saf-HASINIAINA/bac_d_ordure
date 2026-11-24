package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.repository.EntreeAuditRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PurgeJournalAuditTest {

    private static final Clock HORLOGE = Clock.fixed(Instant.parse("2026-11-24T12:00:00Z"), ZoneOffset.UTC);
    private final EntreeAuditRepository repository = mock(EntreeAuditRepository.class);

    @Test
    void supprimeCeQuiDepasseLaDureeDeConservation() {
        when(repository.supprimerAvant(any())).thenReturn(7);

        int supprimees = new PurgeJournalAudit(repository, 90, HORLOGE).purger();

        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(repository).supprimerAvant(limite.capture());
        assertEquals(Instant.parse("2026-08-26T12:00:00Z"), limite.getValue(), "24 novembre moins 90 jours");
        assertEquals(7, supprimees);
    }

    @Test
    void uneDureeNulleOuNegativeDesactiveLaPurge() {
        assertEquals(0, new PurgeJournalAudit(repository, 0, HORLOGE).purger());
        assertEquals(0, new PurgeJournalAudit(repository, -5, HORLOGE).purger());
        verifyNoInteractions(repository);
    }

    @Test
    void uneErreurDeBaseNeSePropagePas() {
        when(repository.supprimerAvant(any())).thenThrow(new IllegalStateException("base injoignable"));
        assertEquals(0, new PurgeJournalAudit(repository, 30, HORLOGE).purger());
    }
}
