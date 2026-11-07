package mg.eni.tulear.ramassage.simulation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PannesSimuleesTest {

    @Test
    void sansTauxDePanneAucunCapteurNeTombeEnPanne() {
        PannesSimulees pannes = new PannesSimulees(0.0, 10);
        for (int i = 0; i < 1000; i++) assertFalse(pannes.enPanne(1L));
    }

    @Test
    void unePanneForceeDureLeNombreDeTicksDemande() {
        PannesSimulees pannes = new PannesSimulees(0.0, 10);
        pannes.forcer(1L, 3);
        assertTrue(pannes.enPanne(1L));
        assertTrue(pannes.enPanne(1L));
        assertTrue(pannes.enPanne(1L));
        assertFalse(pannes.enPanne(1L), "le capteur doit se remettre à émettre après 3 ticks");
    }

    @Test
    void laPanneNeTouchePasLesAutresCapteurs() {
        PannesSimulees pannes = new PannesSimulees(0.0, 10);
        pannes.forcer(1L, 5);
        assertTrue(pannes.enPanne(1L));
        assertFalse(pannes.enPanne(2L));
    }

    @Test
    void avecUnTauxDe100PourCentUneDureeDeNTicksCouvreExactementNTicks() {
        PannesSimulees pannes = new PannesSimulees(1.0, 4);
        // 1er appel : la panne démarre ; les 3 suivants : elle continue ; puis une nouvelle démarre aussitôt (taux 100 %)
        for (int i = 0; i < 8; i++) assertTrue(pannes.enPanne(7L));
    }

    @Test
    void uneDureeDUnTickNeDureQuUnTick() {
        PannesSimulees pannes = new PannesSimulees(1.0, 1);
        assertTrue(pannes.enPanne(1L));
        assertTrue(pannes.enPanne(1L)); // taux 100 % : nouvelle panne à chaque tick
        PannesSimulees sans = new PannesSimulees(0.0, 1);
        sans.forcer(1L, 1);
        assertTrue(sans.enPanne(1L));
        assertFalse(sans.enPanne(1L));
    }
}
