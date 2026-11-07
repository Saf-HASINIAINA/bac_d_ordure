package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.RapportDto;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import mg.eni.tulear.ramassage.repository.CollecteRepository;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RapportServiceTest {

    private CollecteRepository collecteRepository;
    private TourneeRepository tourneeRepository;
    private AlerteRepository alerteRepository;
    private RapportService service;

    private final Zone centre = zone(1L, "Centre-ville");
    private final Zone betania = zone(2L, "Betania");
    private final Chauffeur chauffeur = new Chauffeur("Rasoa", "Paul", "p@tulear.mg", "h", "P1");
    private final Camion camion = new Camion("1234 TAA", 1000, 0, 0);

    private static Zone zone(long id, String nom) {
        Zone z = new Zone(nom, 24, 0, 0);
        ReflectionTestUtils.setField(z, "id", id);
        return z;
    }

    @BeforeEach
    void init() {
        collecteRepository = mock(CollecteRepository.class);
        tourneeRepository = mock(TourneeRepository.class);
        alerteRepository = mock(AlerteRepository.class);
        service = new RapportService(collecteRepository, tourneeRepository, alerteRepository, "Indian/Antananarivo");
        ReflectionTestUtils.setField(chauffeur, "id", 3L);
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of());
        when(tourneeRepository.findByStatutAndDateBetween(any(), any(), any())).thenReturn(List.of());
        when(alerteRepository.compterParType(any(), any())).thenReturn(List.of());
    }

    /** Bac de 240 L : plein, il pèse 36 kg. */
    private Collecte collecte(Zone zone, String code, double poidsKg, String heureUtc) {
        Bac bac = new Bac(code, zone, 0, 0, 240, TypeDechet.MENAGER);
        Tournee t = new Tournee(zone, camion, chauffeur);
        ReflectionTestUtils.setField(t, "id", 10L);
        Collecte c = new Collecte(t, bac, 1);
        ReflectionTestUtils.setField(c, "poidsCollecteKg", poidsKg);
        ReflectionTestUtils.setField(c, "heureCollecte", Instant.parse(heureUtc));
        ReflectionTestUtils.setField(c, "effectuee", true);
        return c;
    }

    private Tournee tourneeTerminee(Zone zone, double km) {
        Tournee t = new Tournee(zone, camion, chauffeur);
        t.setDistanceKm(km);
        ReflectionTestUtils.setField(t, "statut", StatutTournee.TERMINEE);
        return t;
    }

    private static final LocalDate DU = LocalDate.of(2026, 11, 5);
    private static final LocalDate AU = LocalDate.of(2026, 11, 8);

    @Test
    void additionneLeTonnageEtLesCollectes() {
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of(
                collecte(centre, "BAC-1", 36, "2026-11-06T08:00:00Z"),
                collecte(centre, "BAC-2", 18, "2026-11-06T09:00:00Z"),
                collecte(betania, "BAC-3", 9, "2026-11-07T09:00:00Z")));

        RapportDto r = service.generer(DU, AU);

        assertEquals(63, r.tonnageTotalKg(), 1e-9);
        assertEquals(3, r.nombreCollectes());
    }

    @Test
    void lesKilometresEtLesTourneesNeComptentQueLesTourneesTerminees() {
        when(tourneeRepository.findByStatutAndDateBetween(eq(StatutTournee.TERMINEE), eq(DU), eq(AU)))
                .thenReturn(List.of(tourneeTerminee(centre, 4.5), tourneeTerminee(betania, 3.0)));

        RapportDto r = service.generer(DU, AU);

        assertEquals(2, r.nombreTournees());
        assertEquals(7.5, r.kilometresParcourus(), 1e-9);
    }

    @Test
    void leRemplissageALaCollecteSeDeduitDuPoids() {
        // 36 kg = bac plein (100 %), 18 kg = moitié (50 %) -> moyenne 75 %
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of(
                collecte(centre, "BAC-1", 36, "2026-11-06T08:00:00Z"),
                collecte(centre, "BAC-2", 18, "2026-11-06T09:00:00Z")));

        assertEquals(75.0, service.generer(DU, AU).tauxRemplissageMoyenALaCollecte(), 1e-9);
    }

    @Test
    void leRemplissageEstPlafonneA100Pourcent() {
        assertEquals(100.0, RapportService.remplissagePourcent(collecte(centre, "B", 500, "2026-11-06T08:00:00Z")));
    }

    @Test
    void sansActiviteLeRapportEstVideMaisComplet() {
        RapportDto r = service.generer(DU, AU);

        assertEquals(0, r.tonnageTotalKg());
        assertEquals(0, r.nombreCollectes());
        assertEquals(0, r.tauxRemplissageMoyenALaCollecte());
        assertTrue(r.parZone().isEmpty());
        assertEquals(4, r.parJour().size(), "un point par jour, du 5 au 8 novembre inclus");
        assertTrue(r.parJour().stream().allMatch(j -> j.tonnageKg() == 0));
        assertEquals(TypeAlerte.values().length, r.alertesParType().size());
    }

    @Test
    void lesJoursSontDecoupesSelonLeFuseauDeMadagascar() {
        // 22h30 UTC le 6 novembre = 01h30 le 7 novembre à Madagascar (UTC+3)
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of(
                collecte(centre, "BAC-1", 36, "2026-11-06T22:30:00Z")));

        RapportDto r = service.generer(DU, AU);

        RapportDto.ParJour sept = r.parJour().stream().filter(j -> j.jour().equals(LocalDate.of(2026, 11, 7))).findFirst().orElseThrow();
        RapportDto.ParJour six = r.parJour().stream().filter(j -> j.jour().equals(LocalDate.of(2026, 11, 6))).findFirst().orElseThrow();
        assertEquals(36, sept.tonnageKg(), 1e-9);
        assertEquals(0, six.tonnageKg(), 1e-9);
    }

    @Test
    void laPeriodeInterrogeeCommenceEtFinitAMinuitHeureLocale() {
        service.generer(LocalDate.of(2026, 11, 7), LocalDate.of(2026, 11, 7));

        ArgumentCaptor<Instant> de = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> a = ArgumentCaptor.forClass(Instant.class);
        verify(collecteRepository).findEffectueesEntre(de.capture(), a.capture());
        assertEquals(Instant.parse("2026-11-06T21:00:00Z"), de.getValue()); // 7 nov. 00h00 à Madagascar
        assertEquals(Instant.parse("2026-11-07T21:00:00Z"), a.getValue());  // 8 nov. 00h00 (exclu)
    }

    @Test
    void regroupeParZoneEtTriePar_tonnageDecroissant() {
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of(
                collecte(betania, "BAC-1", 10, "2026-11-06T08:00:00Z"),
                collecte(centre, "BAC-2", 30, "2026-11-06T09:00:00Z"),
                collecte(centre, "BAC-3", 6, "2026-11-06T10:00:00Z")));
        when(tourneeRepository.findByStatutAndDateBetween(any(), any(), any()))
                .thenReturn(List.of(tourneeTerminee(centre, 4.0), tourneeTerminee(centre, 2.0)));

        List<RapportDto.ParZone> zones = service.generer(DU, AU).parZone();

        assertEquals(2, zones.size());
        assertEquals("Centre-ville", zones.get(0).zone());
        assertEquals(36, zones.get(0).tonnageKg(), 1e-9);
        assertEquals(2, zones.get(0).collectes());
        assertEquals(2, zones.get(0).tournees());
        assertEquals(6.0, zones.get(0).kilometres(), 1e-9);
        assertEquals("Betania", zones.get(1).zone());
    }

    @Test
    void compteLesAlertesParType() {
        List<Object[]> lignes = new ArrayList<>();
        lignes.add(new Object[] {TypeAlerte.BAC_PLEIN, 12L});
        lignes.add(new Object[] {TypeAlerte.CAPTEUR_PANNE, 3L});
        when(alerteRepository.compterParType(any(), any())).thenReturn(lignes);

        var alertes = service.generer(DU, AU).alertesParType();

        assertEquals(12L, alertes.get(TypeAlerte.BAC_PLEIN));
        assertEquals(3L, alertes.get(TypeAlerte.CAPTEUR_PANNE));
        assertEquals(0L, alertes.get(TypeAlerte.RISQUE_INCENDIE));
    }

    /* ---------- période ---------- */

    @Test
    void parDefautLaPeriodeEstLesTrenteDerniersJours() {
        RapportDto r = service.generer(null, null);
        assertEquals(30, r.parJour().size());
        assertEquals(r.fin().minusDays(29), r.debut());
    }

    @Test
    void unDebutSeulCompleteLaFinAujourdhui() {
        RapportDto r = service.generer(LocalDate.now().minusDays(2), null);
        assertEquals(3, r.parJour().size());
    }

    @Test
    void unDebutApresLaFinEstRefuse() {
        assertThrows(RequeteInvalideException.class, () -> service.generer(AU, DU));
    }

    @Test
    void uneAnneeCompleteEstAcceptee_maisPasPlus() {
        assertDoesNotThrow(() -> service.generer(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)));
        assertThrows(RequeteInvalideException.class,
                () -> service.generer(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 3, 1)));
    }

    @Test
    void lExportCsvContientUneLigneParVidange() {
        when(collecteRepository.findEffectueesEntre(any(), any())).thenReturn(List.of(
                collecte(centre, "BAC-1", 36, "2026-11-06T08:00:00Z"),
                collecte(betania, "BAC-2", 18, "2026-11-06T09:00:00Z")));

        String csv = service.exporterCsv(DU, AU);

        assertEquals(3, csv.split("\r\n").length, "un en-tête + deux vidanges");
        assertEquals("rapport-collectes-2026-11-05_2026-11-08.csv", service.nomFichier(DU, AU));
    }
}
