package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.AlerteDto;
import mg.eni.tulear.ramassage.api.dto.IncidentRequest;
import mg.eni.tulear.ramassage.api.dto.TourneeDto;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import mg.eni.tulear.ramassage.securite.UtilisateurDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TourneeFacadeTest {

    private TourneeService tourneeService;
    private TourneeRepository tourneeRepository;
    private ServiceAlerte serviceAlerte;
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
        serviceAlerte = mock(ServiceAlerte.class);
        facade = new TourneeFacade(tourneeService, tourneeRepository, serviceAlerte);
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

    /* ---------- incidents ---------- */

    private Bac bacDansLaTournee(Tournee t, long id, String code) {
        Bac bac = new Bac(code, t.getZone(), 0, 0, 240, TypeDechet.MENAGER);
        ReflectionTestUtils.setField(bac, "id", id);
        t.ajouterCollecte(new Collecte(t, bac, 1));
        return bac;
    }

    private void alerteRenvoyee() {
        when(serviceAlerte.signalerIncident(any(), any())).thenAnswer(inv ->
                new Alerte(TypeAlerte.INCIDENT_CHAUFFEUR, inv.getArgument(1), inv.getArgument(0)));
    }

    @Test
    void unChauffeurSignaleUnIncidentSurUnBacDeSaTournee() {
        Bac bac = bacDansLaTournee(tourneeDePaul, 50L, "BAC-050");
        alerteRenvoyee();

        AlerteDto dto = facade.signalerIncident(10L, new IncidentRequest("  Accès bloqué par un camion  ", 50L),
                new UtilisateurDetails(3L, "Rasoa", "Paul", "p@tulear.mg", "h", Role.CHAUFFEUR, true));

        verify(serviceAlerte).signalerIncident(eq(bac), argThat(m ->
                m.contains("Paul Rasoa") && m.contains("n°10") && m.contains("BAC-050") && m.contains("Accès bloqué par un camion")));
        assertEquals(TypeAlerte.INCIDENT_CHAUFFEUR, dto.type());
    }

    @Test
    void unIncidentPeutEtreGeneralSansBac() {
        alerteRenvoyee();
        facade.signalerIncident(10L, new IncidentRequest("Panne de camion", null), moi(Role.CHAUFFEUR, 3L));
        verify(serviceAlerte).signalerIncident(eq(null), argThat(m -> m.contains("Panne de camion") && !m.contains("bac ")));
    }

    @Test
    void unChauffeurNePeutPasSignalerUnIncidentSurLaTourneeDUnAutre() {
        assertThrows(AccessDeniedException.class,
                () -> facade.signalerIncident(11L, new IncidentRequest("Panne", null), moi(Role.CHAUFFEUR, 3L)));
        verifyNoInteractions(serviceAlerte);
    }

    @Test
    void unIncidentSurUnBacHorsTourneeEstRefuse() {
        bacDansLaTournee(tourneeDePaul, 50L, "BAC-050");
        assertThrows(RegleMetierException.class,
                () -> facade.signalerIncident(10L, new IncidentRequest("Bac cassé", 99L), moi(Role.CHAUFFEUR, 3L)));
        verifyNoInteractions(serviceAlerte);
    }

    @Test
    void unIncidentSurUneTourneeTermineeEstRefuse() {
        tourneeDePaul.demarrer();
        tourneeDePaul.terminer();
        assertEquals(StatutTournee.TERMINEE, tourneeDePaul.getStatut());
        assertThrows(RegleMetierException.class,
                () -> facade.signalerIncident(10L, new IncidentRequest("Trop tard", null), moi(Role.CHAUFFEUR, 3L)));
    }

    @Test
    void unSuperviseurPeutSignalerUnIncidentSurUneTourneeDeNImporteQui() {
        alerteRenvoyee();
        assertDoesNotThrow(() -> facade.signalerIncident(11L, new IncidentRequest("Constat sur place", null), moi(Role.SUPERVISEUR, 2L)));
    }
}
