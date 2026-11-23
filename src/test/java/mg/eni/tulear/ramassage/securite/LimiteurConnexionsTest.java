package mg.eni.tulear.ramassage.securite;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

class LimiteurConnexionsTest {

    /** Horloge réglable à la main. */
    private static final class HorlogeFixe extends Clock {
        private Instant maintenant = Instant.parse("2026-11-23T10:00:00Z");
        void avancer(Duration d) { maintenant = maintenant.plus(d); }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return maintenant; }
    }

    private final HorlogeFixe horloge = new HorlogeFixe();
    private final LimiteurConnexions limiteur = new LimiteurConnexions(3, Duration.ofMinutes(15), horloge);

    private void echecs(String email, int n) {
        for (int i = 0; i < n; i++) limiteur.enregistrerEchec(email);
    }

    @Test
    void ilFautAtteindreLeNombreMaximalDEchecsPourEtreBloque() {
        echecs("a@tulear.mg", 2);
        assertEquals(Duration.ZERO, limiteur.blocageRestant("a@tulear.mg"));
        echecs("a@tulear.mg", 1);
        assertEquals(Duration.ofMinutes(15), limiteur.blocageRestant("a@tulear.mg"));
    }

    @Test
    void leBlocageSeLeveApresLaDuree() {
        echecs("a@tulear.mg", 3);
        horloge.avancer(Duration.ofMinutes(14));
        assertFalse(limiteur.blocageRestant("a@tulear.mg").isZero());
        horloge.avancer(Duration.ofMinutes(1));
        assertTrue(limiteur.blocageRestant("a@tulear.mg").isZero());
    }

    @Test
    void apresLeBlocageLeCompteurRepartDeZero() {
        echecs("a@tulear.mg", 3);
        horloge.avancer(Duration.ofMinutes(16));
        echecs("a@tulear.mg", 2);
        assertTrue(limiteur.blocageRestant("a@tulear.mg").isZero(), "2 échecs seulement depuis la fin du blocage");
        echecs("a@tulear.mg", 1);
        assertFalse(limiteur.blocageRestant("a@tulear.mg").isZero());
    }

    @Test
    void unSuccesRemetLeCompteurAZero() {
        echecs("a@tulear.mg", 2);
        limiteur.enregistrerSucces("a@tulear.mg");
        echecs("a@tulear.mg", 2);
        assertTrue(limiteur.blocageRestant("a@tulear.mg").isZero());
    }

    @Test
    void desEchecsAnciensHorsFenetreNeSAccumulentPas() {
        echecs("a@tulear.mg", 2);
        horloge.avancer(Duration.ofMinutes(20));
        echecs("a@tulear.mg", 2);
        assertTrue(limiteur.blocageRestant("a@tulear.mg").isZero());
    }

    @Test
    void chaqueAdresseALeSienEtLaCasseEstIgnoree() {
        echecs("A@Tulear.MG ", 3);
        assertFalse(limiteur.blocageRestant("a@tulear.mg").isZero());
        assertTrue(limiteur.blocageRestant("b@tulear.mg").isZero());
    }

    @Test
    void uneAdresseVideOuNulleNeLevePasDException() {
        limiteur.enregistrerEchec(null);
        assertTrue(limiteur.blocageRestant(null).isZero());
    }

    @Test
    void refuseUnMaximumInvalide() {
        assertThrows(IllegalArgumentException.class, () -> new LimiteurConnexions(0, Duration.ofMinutes(1), horloge));
    }
}
