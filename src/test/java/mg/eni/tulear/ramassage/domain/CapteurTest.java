package mg.eni.tulear.ramassage.domain;

import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CapteurTest {

    private Bac bacAvecNiveau(double niveau) {
        Bac bac = new Bac("BAC-T", new Zone("Test", 24, 0, 0), 0, 0, 240, TypeDechet.MENAGER);
        bac.mettreAJourNiveau(niveau, Seuils.parDefaut());
        return bac;
    }

    @Test
    void capteurNiveauNeFaitJamaisBaisserLeNiveauNiDepasser100() {
        Bac bac = bacAvecNiveau(50);
        Capteur capteur = new CapteurNiveau(bac);
        for (int i = 0; i < 500; i++) {
            Mesure m = capteur.mesurer(1.0);
            assertEquals(TypeMesure.NIVEAU, m.getType());
            assertTrue(m.getValeur() >= bac.getNiveauPourcent() && m.getValeur() <= 100);
            bac.mettreAJourNiveau(m.getValeur(), Seuils.parDefaut());
        }
        assertEquals(100, bac.getNiveauPourcent());
    }

    @Test
    void capteurPoidsCalculeLePoidsSelonLeVolume() {
        Bac bac = bacAvecNiveau(50); // 50 % de 240 L = 120 L x 0,15 kg/L = 18 kg
        Mesure m = new CapteurPoids(bac).mesurer();
        assertEquals(TypeMesure.POIDS, m.getType());
        assertEquals(18.0, m.getValeur(), 1e-9);
    }

    @Test
    void capteurGazResteDansUneplageCoherente() {
        Capteur capteur = new CapteurGaz(bacAvecNiveau(10));
        for (int i = 0; i < 1000; i++) {
            Mesure m = capteur.mesurer();
            assertEquals(TypeMesure.GAZ, m.getType());
            assertTrue(m.getValeur() >= 20 && m.getValeur() < 500);
        }
    }

    @Test
    void factoryCreeLeBonTypeDeCapteur() {
        Bac bac = bacAvecNiveau(0);
        assertInstanceOf(CapteurNiveau.class, CapteurFactory.creer(TypeCapteur.NIVEAU, bac));
        assertInstanceOf(CapteurPoids.class, CapteurFactory.creer(TypeCapteur.POIDS, bac));
        assertInstanceOf(CapteurGaz.class, CapteurFactory.creer(TypeCapteur.GAZ, bac));
    }
}
