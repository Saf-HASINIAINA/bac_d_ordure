package mg.eni.tulear.ramassage.planification;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class PlanificateurTourneeTest {

    private static final Position DEPOT = new Position(-23.35, 43.67);
    private final Seuils seuils = Seuils.parDefaut();
    private final Zone zone = new Zone("Test", 24, -23.35, 43.67);
    private final Chauffeur chauffeur = new Chauffeur("A", "B", "c@d.mg", "h", "P1");
    private PlanificateurTournee planificateur;

    @BeforeEach
    void init() {
        planificateur = new PlanificateurTournee(new PlusProcheVoisin());
    }

    /** Bac de 240 L : à 100 % il pèse 36 kg. */
    private Bac bac(String code, double longitude, double niveau) {
        Bac b = new Bac(code, zone, -23.35, longitude, 240, TypeDechet.MENAGER);
        b.mettreAJourNiveau(niveau, seuils);
        return b;
    }

    private Camion camion(double capaciteKg) {
        return new Camion("1234 TAA", capaciteKg, DEPOT.latitude(), DEPOT.longitude());
    }

    private List<String> codes(Tournee t) {
        return t.getCollectes().stream().map(c -> c.getBac().getCode()).toList();
    }

    @Test
    void neRetientQueLesBacsAuDessusDuSeuilRG2() {
        List<Bac> bacs = List.of(bac("PLEIN", 43.68, 90), bac("MOYEN", 43.69, 50));
        Tournee t = planificateur.planifier(zone, bacs, camion(1000), chauffeur, DEPOT, seuils).orElseThrow();
        assertEquals(List.of("PLEIN"), codes(t));
    }

    @Test
    void ignoreLesBacsHorsService() {
        Bac hs = bac("HS", 43.68, 95);
        hs.setEtat(EtatBac.HORS_SERVICE);
        assertTrue(planificateur.planifier(zone, List.of(hs), camion(1000), chauffeur, DEPOT, seuils).isEmpty());
    }

    @Test
    void aucunBacACollecterDonneAucuneTournee() {
        Optional<Tournee> t = planificateur.planifier(zone, List.of(bac("A", 43.68, 10)), camion(1000), chauffeur, DEPOT, seuils);
        assertTrue(t.isEmpty());
    }

    @Test
    void respecteLaCapaciteDuCamionRG3() {
        // 3 bacs pleins de 36 kg = 108 kg ; camion de 80 kg => 2 bacs maximum
        List<Bac> bacs = List.of(bac("A", 43.68, 100), bac("B", 43.69, 100), bac("C", 43.70, 100));
        Tournee t = planificateur.planifier(zone, bacs, camion(80), chauffeur, DEPOT, seuils).orElseThrow();
        assertEquals(2, t.getCollectes().size());
    }

    @Test
    void lesBacsLesPlusRemplisSontPrioritairesQuandLaPlaceManqueRG5() {
        List<Bac> bacs = List.of(bac("85", 43.68, 85), bac("100", 43.69, 100), bac("90", 43.70, 90));
        Tournee t = planificateur.planifier(zone, bacs, camion(80), chauffeur, DEPOT, seuils).orElseThrow();
        assertTrue(codes(t).containsAll(List.of("100", "90")));
        assertFalse(codes(t).contains("85"));
    }

    @Test
    void plusProcheVoisinVisiteLesBacsDeProcheEnProche() {
        List<Bac> bacs = List.of(bac("LOIN", 43.90, 90), bac("PRES", 43.68, 90), bac("MILIEU", 43.75, 90));
        Tournee t = planificateur.planifier(zone, bacs, camion(1000), chauffeur, DEPOT, seuils).orElseThrow();
        assertEquals(List.of("PRES", "MILIEU", "LOIN"), codes(t));
        assertEquals(List.of(1, 2, 3), t.getCollectes().stream().map(Collecte::getOrdre).toList());
    }

    @Test
    void prioriteRemplissageVisiteLePlusPleinEnPremier() {
        planificateur.definirStrategie(new PrioriteRemplissage());
        List<Bac> bacs = List.of(bac("PRES", 43.68, 85), bac("LOIN", 43.90, 99), bac("MILIEU", 43.75, 92));
        Tournee t = planificateur.planifier(zone, bacs, camion(1000), chauffeur, DEPOT, seuils).orElseThrow();
        assertEquals(List.of("LOIN", "MILIEU", "PRES"), codes(t));
    }

    @Test
    void distanceInclutLeRetourAuDepot() {
        Bac b = bac("A", 43.77, 90); // environ 10 km à l'est du dépôt
        Tournee t = planificateur.planifier(zone, List.of(b), camion(1000), chauffeur, DEPOT, seuils).orElseThrow();
        assertEquals(2 * DEPOT.distanceVers(b.getPosition()), t.getDistanceKm(), 1e-9);
    }

    @Test
    void laCapaciteTientCompteDeLaChargeDejaPresente() {
        Camion c = camion(100);
        c.charger(70); // il ne reste que 30 kg : aucun bac de 36 kg ne rentre
        assertTrue(planificateur.planifier(zone, List.of(bac("A", 43.68, 100)), c, chauffeur, DEPOT, seuils).isEmpty());
    }
}
