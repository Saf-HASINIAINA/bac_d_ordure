package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TourneeFacadeTest {

    private TourneeService tourneeService;
    private TourneeRepository tourneeRepository;
    private TourneeFacade facade;
    private Tournee tourneeDePaul;
    private Tournee tourneeDeMarie;

    private static UtilisateurDetails moi(Role role, long id) {
        return new UtilisateurDetails(id, "Nom", "Prenom", "x@tulear.mg", "h", role, true);
    }

    private Tournee tournee(long id, long chauffeurId) {
        Zone zone = new Zone("Zone", 24, 0, 0);
        Camion camion = new Camion("1234 TAA", 1000, 0, 0);
        Chauffeur chauffeur = new Chauffeur("Nom" + chauffeurId, "Prenom", "c" + chauffeurId + "@tulear.mg", "h", "P");
        ReflectionTestUtils.setField(zone, "id", 1L);
        ReflectionTestUtils.setField(camion, "id", 1L);
        ReflectionTestUtils.setField(chauffeur, "id", chauffeurId);
        Tournee t = new Tournee(zone, camion, chauffeur);
        ReflectionTestUtils.setField(t, "id", id);
        return t;
    }

    @BeforeEach
    void init() {
        tourneeService = mock(TourneeService.class);
        tourneeRepository = mock(TourneeRepository.class);
        facade = new TourneeFacade(tourneeService, tourneeRepository);
        tourneeDePaul = tournee(10L, 3L);
        tourneeDeMarie = tournee(11L, 4L);
        when(tourneeRepository.findAllByOrderByIdDesc()).thenReturn(List.of(tourneeDeMarie, tourneeDePaul));
        when(tourneeRepository.findById(10L)).thenReturn(Optional.of(tourneeDePaul));
        when(tourneeRepository.findById(11L)).thenReturn(Optional.of(tourneeDeMarie));
    }

    @Test
    void unChauffeurNeVoitQueSesTournees() {
        List<TourneeDto> vues = facade.lister(null, moi(Role.CHAUFFEUR, 3L));
        assertEquals(1, vues.size());
        assertEquals(10L, vues.get(0).id());
    }

    @Test
    void unSuperviseurVoitToutesLesTournees() {
        assertEquals(2, facade.lister(null, moi(Role.SUPERVISEUR, 2L)).size());
        assertEquals(2, facade.lister(null, moi(Role.ADMIN, 1L)).size());
    }

    @Test
    void unChauffeurNePeutPasConsulterLaTourneeDUnAutre() {
        assertThrows(AccessDeniedException.class, () -> facade.obtenir(11L, moi(Role.CHAUFFEUR, 3L)));
        assertEquals(10L, facade.obtenir(10L, moi(Role.CHAUFFEUR, 3L)).id());
    }

    @Test
    void unChauffeurNePeutPasDemarrerVidangerNiTerminerLaTourneeDUnAutre() {
        UtilisateurDetails paul = moi(Role.CHAUFFEUR, 3L);
        assertThrows(AccessDeniedException.class, () -> facade.demarrer(11L, paul));
        assertThrows(AccessDeniedException.class, () -> facade.confirmerCollecte(11L, 5L, paul));
        assertThrows(AccessDeniedException.class, () -> facade.terminer(11L, paul));
        verifyNoInteractions(tourneeService);
    }

    @Test
    void unChauffeurPeutDemarrerSaPropreTournee() {
        when(tourneeService.demarrer(10L)).thenReturn(tourneeDePaul);
        assertEquals(10L, facade.demarrer(10L, moi(Role.CHAUFFEUR, 3L)).id());
        verify(tourneeService).demarrer(10L);
    }

    @Test
    void unSuperviseurPeutAgirSurLaTourneeDeNImporteQuiChauffeur() {
        when(tourneeService.demarrer(11L)).thenReturn(tourneeDeMarie);
        assertEquals(11L, facade.demarrer(11L, moi(Role.SUPERVISEUR, 2L)).id());
    }
}
