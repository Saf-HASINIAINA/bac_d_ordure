package mg.eni.tulear.ramassage.domain;

import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BacTest {

    private final Seuils seuils = Seuils.parDefaut();
    private final Zone zone = new Zone("Test", 24, -23.35, 43.67);

    private Bac nouveauBac() {
        return new Bac("BAC-T", zone, -23.35, 43.67, 240, TypeDechet.MENAGER);
    }

    @Test
    void etatSuitLeNiveauSelonRG1() {
        assertEquals(EtatBac.VIDE, seuils.etatPour(10));
        assertEquals(EtatBac.MOYEN, seuils.etatPour(40));
        assertEquals(EtatBac.PLEIN, seuils.etatPour(70));
        assertEquals(EtatBac.CRITIQUE, seuils.etatPour(95));
    }

    @Test
    void niveauEstBorneEntre0Et100() {
        Bac bac = nouveauBac();
        bac.mettreAJourNiveau(150, seuils);
        assertEquals(100, bac.getNiveauPourcent());
        assertEquals(EtatBac.CRITIQUE, bac.getEtat());
        bac.mettreAJourNiveau(-5, seuils);
        assertEquals(0, bac.getNiveauPourcent());
    }

    @Test
    void viderRemetLeNiveauAZeroRG7() {
        Bac bac = nouveauBac();
        bac.mettreAJourNiveau(85, seuils);
        bac.vider();
        assertEquals(0, bac.getNiveauPourcent());
        assertEquals(EtatBac.VIDE, bac.getEtat());
        assertNotNull(bac.getDateDerniereCollecte());
    }

    @Test
    void bacHorsServiceGardeSonEtatEtN_estPasACollecter() {
        Bac bac = nouveauBac();
        bac.setEtat(EtatBac.HORS_SERVICE);
        bac.mettreAJourNiveau(95, seuils);
        assertEquals(EtatBac.HORS_SERVICE, bac.getEtat());
        assertFalse(bac.estACollecter(80));
    }

    @Test
    void estACollecterAuDelaDuSeuil() {
        Bac bac = nouveauBac();
        bac.mettreAJourNiveau(81, seuils);
        assertTrue(bac.estACollecter(80));
        assertFalse(bac.estACollecter(90));
    }

    @Test
    void horsServiceEtRemiseEnServiceRecalculeLEtat() {
        Bac bac = nouveauBac();
        bac.mettreAJourNiveau(85, seuils);
        bac.mettreHorsService();
        assertEquals(EtatBac.HORS_SERVICE, bac.getEtat());
        bac.mettreAJourNiveau(95, seuils);
        assertEquals(EtatBac.HORS_SERVICE, bac.getEtat(), "un bac en maintenance garde son état même si le niveau monte");
        bac.remettreEnService(seuils);
        assertEquals(EtatBac.CRITIQUE, bac.getEtat());
    }

    @Test
    void modifierChangeLeBacMaisPasSonCode() {
        Bac bac = nouveauBac();
        Zone autre = new Zone("Autre", 12, 0, 0);
        bac.modifier(autre, -23.4, 43.7, 500, TypeDechet.RECYCLABLE);
        assertEquals("BAC-T", bac.getCode());
        assertSame(autre, bac.getZone());
        assertEquals(500, bac.getCapaciteLitres());
        assertEquals(TypeDechet.RECYCLABLE, bac.getTypeDechet());
        assertEquals(-23.4, bac.getLatitude());
    }
}
