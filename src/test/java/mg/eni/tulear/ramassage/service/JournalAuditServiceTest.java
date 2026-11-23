package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.PageAuditDto;
import mg.eni.tulear.ramassage.domain.EntreeAudit;
import mg.eni.tulear.ramassage.repository.EntreeAuditRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JournalAuditServiceTest {

    private EntreeAuditRepository repository;
    private JournalAuditService service;

    @BeforeEach
    void init() {
        repository = mock(EntreeAuditRepository.class);
        service = new JournalAuditService(repository);
    }

    @Test
    void enregistreTousLesChamps() {
        service.enregistrer(3L, "paul@tulear.mg", "Démarrage d'une tournée", "POST", "/api/tournees/5/demarrer", 200, "10.0.0.1");

        ArgumentCaptor<EntreeAudit> c = ArgumentCaptor.forClass(EntreeAudit.class);
        verify(repository).save(c.capture());
        EntreeAudit e = c.getValue();
        assertEquals(3L, e.getUtilisateurId());
        assertEquals("paul@tulear.mg", e.getEmail());
        assertEquals("Démarrage d'une tournée", e.getAction());
        assertEquals("/api/tournees/5/demarrer", e.getChemin());
        assertEquals(200, e.getStatut());
        assertNotNull(e.getDateAction());
    }

    @Test
    void uneActionAnonymeEstEtiqueteeAnonyme() {
        service.enregistrer(null, null, "Inscription d'un compte", "POST", "/api/auth/inscription", 201, "10.0.0.1");
        ArgumentCaptor<EntreeAudit> c = ArgumentCaptor.forClass(EntreeAudit.class);
        verify(repository).save(c.capture());
        assertEquals("anonyme", c.getValue().getEmail());
        assertNull(c.getValue().getUtilisateurId());
    }

    @Test
    void uneErreurDeLaBaseNeFaitJamaisEchouerLActionDeLUtilisateur() {
        when(repository.save(any())).thenThrow(new DataAccessResourceFailureException("base injoignable"));
        assertDoesNotThrow(() -> service.enregistrer(1L, "a@b.mg", "x", "POST", "/api/x", 200, null));
    }

    @Test
    void listerBorneLaTailleDeLaPageEtLaPageMinimale() {
        when(repository.findAll(any(Pageable.class))).thenAnswer(inv -> new PageImpl<EntreeAudit>(List.of(), inv.getArgument(0), 0));

        PageAuditDto grande = service.lister(-3, 100_000);
        assertEquals(0, grande.page());
        assertEquals(JournalAuditService.TAILLE_MAX, grande.taille());

        assertEquals(1, service.lister(0, 0).taille());
    }

    @Test
    void listerTrieDuPlusRecentAuPlusAncien() {
        when(repository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        service.lister(0, 50);
        ArgumentCaptor<Pageable> c = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findAll(c.capture());
        assertEquals("dateAction: DESC,id: DESC", c.getValue().getSort().toString());
    }
}
