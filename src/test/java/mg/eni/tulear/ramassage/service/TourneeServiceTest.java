package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.StatutCamion;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.planification.PlanificateurTournee;
import mg.eni.tulear.ramassage.planification.PlusProcheVoisin;
import mg.eni.tulear.ramassage.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TourneeServiceTest {

    private final Seuils seuils = Seuils.parDefaut();
    private ZoneRepository zoneRepository;
    private CamionRepository camionRepository;
    private UtilisateurRepository utilisateurRepository;
    private BacRepository bacRepository;
    private TourneeRepository tourneeRepository;
    private CollecteRepository collecteRepository;
    private AlerteRepository alerteRepository;
    private ServiceAlerte serviceAlerte;
    private TourneeService service;

    private Zone zone;
    private Camion camion;
    private Chauffeur chauffeur;
    private Bac bacPlein;
    private Bac bacMoyen;

    @BeforeEach
    void init() {
        zoneRepository = mock(ZoneRepository.class);
        camionRepository = mock(CamionRepository.class);
        utilisateurRepository = mock(UtilisateurRepository.class);
        bacRepository = mock(BacRepository.class);
        tourneeRepository = mock(TourneeRepository.class);
        collecteRepository = mock(CollecteRepository.class);
        alerteRepository = mock(AlerteRepository.class);
        serviceAlerte = mock(ServiceAlerte.class);
        ParametreService parametreService = mock(ParametreService.class);
        when(parametreService.chargerSeuils()).thenReturn(seuils);

        service = new TourneeService(zoneRepository, camionRepository, utilisateurRepository, bacRepository,
                tourneeRepository, collecteRepository, alerteRepository, parametreService, serviceAlerte,
                new PlanificateurTournee(new PlusProcheVoisin()), -23.35, 43.67);

        zone = new Zone("Test", 24, -23.35, 43.67);
        camion = new Camion("1234 TAA", 1000, -23.35, 43.67);
        chauffeur = new Chauffeur("A", "B", "c@d.mg", "h", "P1");
        bacPlein = bac(10L, "PLEIN", 43.68, 100);
        bacMoyen = bac(11L, "MOYEN", 43.69, 50);
        ReflectionTestUtils.setField(zone, "id", 1L);
        ReflectionTestUtils.setField(camion, "id", 2L);
        ReflectionTestUtils.setField(chauffeur, "id", 3L);

        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(camionRepository.findById(2L)).thenReturn(Optional.of(camion));
        when(utilisateurRepository.findById(3L)).thenReturn(Optional.of(chauffeur));
        when(bacRepository.findByZoneId(1L)).thenReturn(List.of(bacPlein, bacMoyen));
        when(collecteRepository.findBacIdsEnTournee(any())).thenReturn(List.of());
        when(tourneeRepository.save(any(Tournee.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Bac bac(Long id, String code, double longitude, double niveau) {
        Bac b = new Bac(code, zone, -23.35, longitude, 240, TypeDechet.MENAGER);
        b.mettreAJourNiveau(niveau, seuils);
        ReflectionTestUtils.setField(b, "id", id);
        return b;
    }

    private Tournee tourneeEnCours() {
        Tournee t = service.planifier(1L, 2L, 3L, null);
        when(tourneeRepository.findById(5L)).thenReturn(Optional.of(t));
        service.demarrer(5L);
        return t;
    }

    @Test
    void planifierCreeUneTourneeAvecLesBacsACollecter() {
        Tournee t = service.planifier(1L, 2L, 3L, null);
        assertEquals(StatutTournee.PLANIFIEE, t.getStatut());
        assertEquals(1, t.getCollectes().size());
        assertEquals("PLEIN", t.getCollectes().get(0).getBac().getCode());
    }

    @Test
    void planifierExclutLesBacsDejaPrevusDansUneAutreTournee() {
        when(collecteRepository.findBacIdsEnTournee(any())).thenReturn(List.of(10L));
        RegleMetierException e = assertThrows(RegleMetierException.class, () -> service.planifier(1L, 2L, 3L, null));
        assertTrue(e.getMessage().contains("Aucun bac"));
    }

    @Test
    void planifierRefuseUnCamionIndisponibleRG4() {
        camion.setStatut(StatutCamion.EN_PANNE);
        assertThrows(RegleMetierException.class, () -> service.planifier(1L, 2L, 3L, null));
    }

    @Test
    void planifierRefuseUnChauffeurDejaOccupeRG4() {
        when(tourneeRepository.existsByChauffeurIdAndStatutIn(eq(3L), any())).thenReturn(true);
        assertThrows(RegleMetierException.class, () -> service.planifier(1L, 2L, 3L, null));
    }

    @Test
    void demarrerMetLeCamionEnTournee() {
        tourneeEnCours();
        assertEquals(StatutCamion.EN_TOURNEE, camion.getStatut());
    }

    @Test
    void confirmerCollecteVideLeBacChargeLeCamionEtClotureLAlerteRG7() {
        tourneeEnCours();
        Alerte alerte = new Alerte(TypeAlerte.BAC_PLEIN, "plein", bacPlein);
        when(alerteRepository.findByBacIdAndTypeAndTraiteeFalse(10L, TypeAlerte.BAC_PLEIN)).thenReturn(List.of(alerte));

        Collecte c = service.confirmerCollecte(5L, 10L);

        assertTrue(c.isEffectuee());
        assertEquals(36.0, c.getPoidsCollecteKg(), 1e-9);
        assertEquals(0, bacPlein.getNiveauPourcent());
        assertEquals(36.0, camion.getChargeActuelleKg(), 1e-9);
        assertTrue(alerte.isTraitee());
        verify(serviceAlerte).alerteTraitee(10L, TypeAlerte.BAC_PLEIN);
    }

    @Test
    void onNePeutPasViderDeuxFoisLeMemeBac() {
        tourneeEnCours();
        service.confirmerCollecte(5L, 10L);
        assertThrows(RegleMetierException.class, () -> service.confirmerCollecte(5L, 10L));
    }

    @Test
    void onNePeutPasViderUnBacHorsTournee() {
        tourneeEnCours();
        assertThrows(RegleMetierException.class, () -> service.confirmerCollecte(5L, 11L));
    }

    @Test
    void confirmerExigeUneTourneeEnCours() {
        Tournee t = service.planifier(1L, 2L, 3L, null);
        when(tourneeRepository.findById(5L)).thenReturn(Optional.of(t));
        assertThrows(RegleMetierException.class, () -> service.confirmerCollecte(5L, 10L));
    }

    @Test
    void terminerDechargeEtLibereLeCamion() {
        Tournee t = tourneeEnCours();
        service.confirmerCollecte(5L, 10L);
        service.terminer(5L);
        assertEquals(StatutTournee.TERMINEE, t.getStatut());
        assertEquals(0, camion.getChargeActuelleKg());
        assertEquals(StatutCamion.DISPONIBLE, camion.getStatut());
    }

    @Test
    void annulerUneTourneeDejaDemarreeEstRefuse() {
        tourneeEnCours();
        assertThrows(RegleMetierException.class, () -> service.annuler(5L));
    }
}
