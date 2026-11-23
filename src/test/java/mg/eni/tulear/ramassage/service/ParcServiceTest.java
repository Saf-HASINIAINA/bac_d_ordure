package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import mg.eni.tulear.ramassage.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ParcServiceTest {

    private BacRepository bacRepository;
    private CapteurRepository capteurRepository;
    private ZoneRepository zoneRepository;
    private CamionRepository camionRepository;
    private TourneeRepository tourneeRepository;
    private CollecteRepository collecteRepository;
    private ServiceAlerte serviceAlerte;
    private ParcService parc;

    private Zone zone;

    @BeforeEach
    void init() {
        bacRepository = mock(BacRepository.class);
        capteurRepository = mock(CapteurRepository.class);
        zoneRepository = mock(ZoneRepository.class);
        camionRepository = mock(CamionRepository.class);
        tourneeRepository = mock(TourneeRepository.class);
        collecteRepository = mock(CollecteRepository.class);
        serviceAlerte = mock(ServiceAlerte.class);
        ParametreService parametreService = mock(ParametreService.class);
        when(parametreService.chargerSeuils()).thenReturn(Seuils.parDefaut());
        parc = new ParcService(bacRepository, capteurRepository, zoneRepository, camionRepository, tourneeRepository,
                collecteRepository, parametreService, serviceAlerte, -23.355, 43.67);

        zone = new Zone("Centre-ville", 12, -23.35, 43.67);
        ReflectionTestUtils.setField(zone, "id", 1L);
        when(zoneRepository.findById(1L)).thenReturn(Optional.of(zone));
        when(bacRepository.save(any(Bac.class))).thenAnswer(inv -> inv.getArgument(0));
        when(zoneRepository.save(any(Zone.class))).thenAnswer(inv -> inv.getArgument(0));
        when(camionRepository.save(any(Camion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collecteRepository.findBacIdsEnTournee(any())).thenReturn(List.of());
    }

    private BacRequest requeteBac(String code) {
        return new BacRequest(code, 1L, -23.35, 43.67, 240.0, null);
    }

    private Bac bacExistant(long id, String code) {
        Bac b = new Bac(code, zone, 0, 0, 240, TypeDechet.MENAGER);
        ReflectionTestUtils.setField(b, "id", id);
        when(bacRepository.findById(id)).thenReturn(Optional.of(b));
        return b;
    }

    private Camion camionExistant(long id, String immat, double capacite) {
        Camion c = new Camion(immat, capacite, 0, 0);
        ReflectionTestUtils.setField(c, "id", id);
        when(camionRepository.findById(id)).thenReturn(Optional.of(c));
        return c;
    }

    /* ---------- installation d'un bac ---------- */

    @Test
    void installerUnBacCreeUnBacVideAvecSesTroisCapteurs() {
        BacDto dto = parc.installerBac(requeteBac("bac-x1"));

        assertEquals("BAC-X1", dto.code(), "code normalisé en majuscules");
        assertEquals(0, dto.niveauPourcent());
        assertEquals(EtatBac.VIDE, dto.etat());
        assertEquals(TypeDechet.MENAGER, dto.typeDechet(), "type par défaut");

        ArgumentCaptor<Capteur> capteurs = ArgumentCaptor.forClass(Capteur.class);
        verify(capteurRepository, times(3)).save(capteurs.capture());
        assertEquals(List.of(TypeCapteur.NIVEAU, TypeCapteur.POIDS, TypeCapteur.GAZ),
                capteurs.getAllValues().stream().map(Capteur::getType).toList());
        assertTrue(capteurs.getAllValues().stream().allMatch(Capteur::isActif));
    }

    @Test
    void sansCodeLeCodeEstGenereEtJamaisEnDoublon() {
        when(bacRepository.count()).thenReturn(4L);
        when(bacRepository.existsByCode("BAC-005")).thenReturn(true);

        assertEquals("BAC-006", parc.installerBac(requeteBac(null)).code());
        assertEquals("BAC-006", parc.installerBac(requeteBac("  ")).code());
    }

    @Test
    void unCodeDejaUtiliseEstRefuse() {
        when(bacRepository.existsByCode("BAC-001")).thenReturn(true);
        RegleMetierException e = assertThrows(RegleMetierException.class, () -> parc.installerBac(requeteBac("bac-001")));
        assertTrue(e.getMessage().contains("BAC-001"));
        verify(bacRepository, never()).save(any());
        verifyNoInteractions(capteurRepository);
    }

    @Test
    void installerDansUneZoneInconnueRenvoieIntrouvable() {
        BacRequest r = new BacRequest(null, 99L, 0.0, 0.0, 240.0, TypeDechet.MENAGER);
        when(zoneRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> parc.installerBac(r));
    }

    /* ---------- modification, suppression, maintenance d'un bac ---------- */

    @Test
    void modifierUnBacGardeSonCode() {
        Bac bac = bacExistant(10L, "BAC-010");
        BacDto dto = parc.modifierBac(10L, new BacRequest("AUTRE", 1L, -23.4, 43.7, 500.0, TypeDechet.ORGANIQUE));
        assertEquals("BAC-010", dto.code());
        assertEquals(500, bac.getCapaciteLitres());
        assertEquals(TypeDechet.ORGANIQUE, bac.getTypeDechet());
    }

    @Test
    void unBacDejaCollecteNePeutPasEtreSupprime() {
        bacExistant(10L, "BAC-010");
        when(collecteRepository.existsByBacId(10L)).thenReturn(true);
        RegleMetierException e = assertThrows(RegleMetierException.class, () -> parc.supprimerBac(10L));
        assertTrue(e.getMessage().contains("hors service"));
        verify(bacRepository, never()).delete(any(Bac.class));
    }

    @Test
    void unBacJamaisCollecteEstSupprimeEtLaMemoireDesAlertesNettoyee() {
        Bac bac = bacExistant(10L, "BAC-010");
        parc.supprimerBac(10L);
        verify(bacRepository).delete(bac);
        for (TypeAlerte type : TypeAlerte.values()) verify(serviceAlerte).alerteTraitee(10L, type);
    }

    @Test
    void mettreHorsServiceEtRemettreEnService() {
        Bac bac = bacExistant(10L, "BAC-010");
        bac.mettreAJourNiveau(85, Seuils.parDefaut());

        assertEquals(EtatBac.HORS_SERVICE, parc.mettreBacHorsService(10L).etat());
        assertThrows(RegleMetierException.class, () -> parc.mettreBacHorsService(10L), "déjà hors service");
        assertEquals(EtatBac.PLEIN, parc.remettreBacEnService(10L).etat(), "l'état est recalculé d'après le niveau");
        assertThrows(RegleMetierException.class, () -> parc.remettreBacEnService(10L), "déjà en service");
    }

    @Test
    void unBacPrevuDansUneTourneeNePeutPasPasserHorsService() {
        Bac bac = bacExistant(10L, "BAC-010");
        when(collecteRepository.findBacIdsEnTournee(any())).thenReturn(List.of(10L));
        assertThrows(RegleMetierException.class, () -> parc.mettreBacHorsService(10L));
        assertNotEquals(EtatBac.HORS_SERVICE, bac.getEtat());
    }

    /* ---------- zones ---------- */

    @Test
    void creerUneZone() {
        ZoneDto dto = parc.creerZone(new ZoneRequest("  Mahavatse ", 24, -23.36, 43.68));
        assertEquals("Mahavatse", dto.nom());
        assertEquals(24, dto.frequenceCollecteHeures());
        assertEquals(-23.36, dto.latitudeCentre());
        assertEquals(0, dto.nombreBacs());
    }

    @Test
    void unNomDeZoneDejaPrisEstRefuse() {
        when(zoneRepository.findByNomIgnoreCase("Centre-ville")).thenReturn(Optional.of(zone));
        assertThrows(RegleMetierException.class, () -> parc.creerZone(new ZoneRequest("Centre-ville", 24, null, null)));
    }

    @Test
    void modifierUneZoneAvecSonPropreNomEstPermis() {
        when(zoneRepository.findByNomIgnoreCase("Centre-ville")).thenReturn(Optional.of(zone));
        ZoneDto dto = parc.modifierZone(1L, new ZoneRequest("Centre-ville", 6, null, null));
        assertEquals(6, dto.frequenceCollecteHeures());
    }

    @Test
    void modifierUneZoneVersLeNomDUneAutreEstRefuse() {
        Zone autre = new Zone("Betania", 24, 0, 0);
        ReflectionTestUtils.setField(autre, "id", 2L);
        when(zoneRepository.findByNomIgnoreCase("Betania")).thenReturn(Optional.of(autre));
        assertThrows(RegleMetierException.class, () -> parc.modifierZone(1L, new ZoneRequest("Betania", 24, null, null)));
    }

    @Test
    void uneZoneAvecDesBacsOuUnHistoriqueNePeutPasEtreSupprimee() {
        when(bacRepository.countByZoneId(1L)).thenReturn(3L);
        RegleMetierException e = assertThrows(RegleMetierException.class, () -> parc.supprimerZone(1L));
        assertTrue(e.getMessage().contains("3 bac"));

        when(bacRepository.countByZoneId(1L)).thenReturn(0L);
        when(tourneeRepository.existsByZoneId(1L)).thenReturn(true);
        assertThrows(RegleMetierException.class, () -> parc.supprimerZone(1L));
        verify(zoneRepository, never()).delete(any());
    }

    @Test
    void uneZoneVideEtSansHistoriqueEstSupprimee() {
        parc.supprimerZone(1L);
        verify(zoneRepository).delete(zone);
    }

    /* ---------- camions ---------- */

    @Test
    void creerUnCamionLePlaceAuDepotEtNormaliseLImmatriculation() {
        CamionDto dto = parc.creerCamion(new CamionRequest(" 9999 taa ", 6000.0));
        assertEquals("9999 TAA", dto.immatriculation());
        assertEquals(StatutCamion.DISPONIBLE, dto.statut());
        assertEquals(-23.355, dto.latitude());
        assertEquals(43.67, dto.longitude());
    }

    @Test
    void uneImmatriculationDejaUtiliseeEstRefusee() {
        when(camionRepository.findByImmatriculationIgnoreCase("1234 TAA")).thenReturn(Optional.of(new Camion("1234 TAA", 1, 0, 0)));
        assertThrows(RegleMetierException.class, () -> parc.creerCamion(new CamionRequest("1234 taa", 5000.0)));
    }

    @Test
    void laCapaciteNePeutPasPasserSousLaChargeActuelle() {
        Camion c = camionExistant(5L, "1234 TAA", 5000);
        c.charger(3000);
        assertThrows(RegleMetierException.class, () -> parc.modifierCamion(5L, new CamionRequest("1234 TAA", 2000.0)));
        assertEquals(5000, c.getCapaciteKg());
        assertEquals(8000, parc.modifierCamion(5L, new CamionRequest("1234 TAA", 8000.0)).capaciteKg());
    }

    @Test
    void unCamionAvecUnHistoriqueNePeutPasEtreSupprime() {
        Camion c = camionExistant(5L, "1234 TAA", 5000);
        when(tourneeRepository.existsByCamionId(5L)).thenReturn(true);
        assertThrows(RegleMetierException.class, () -> parc.supprimerCamion(5L));
        when(tourneeRepository.existsByCamionId(5L)).thenReturn(false);
        parc.supprimerCamion(5L);
        verify(camionRepository).delete(c);
    }

    @Test
    void declarerEnPanneEtRemettreDisponible() {
        Camion c = camionExistant(5L, "1234 TAA", 5000);

        assertEquals(StatutCamion.EN_PANNE, parc.declarerCamionEnPanne(5L).statut());
        assertThrows(RegleMetierException.class, () -> parc.declarerCamionEnPanne(5L), "déjà en panne");
        assertEquals(StatutCamion.DISPONIBLE, parc.remettreCamionDisponible(5L).statut());
        assertThrows(RegleMetierException.class, () -> parc.remettreCamionDisponible(5L), "pas en panne");
        assertEquals(StatutCamion.DISPONIBLE, c.getStatut());
    }

    @Test
    void unCamionEnTourneeOuAffecteNePeutPasEtreDeclareEnPanne() {
        Camion c = camionExistant(5L, "1234 TAA", 5000);
        c.setStatut(StatutCamion.EN_TOURNEE);
        assertThrows(RegleMetierException.class, () -> parc.declarerCamionEnPanne(5L));

        c.setStatut(StatutCamion.DISPONIBLE);
        when(tourneeRepository.existsByCamionIdAndStatutIn(eq5(), any())).thenReturn(true);
        assertThrows(RegleMetierException.class, () -> parc.declarerCamionEnPanne(5L), "affecté à une tournée planifiée");
        assertEquals(StatutCamion.DISPONIBLE, c.getStatut());
    }

    private static Long eq5() {
        return org.mockito.ArgumentMatchers.eq(5L);
    }
}
