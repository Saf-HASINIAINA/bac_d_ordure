package mg.eni.tulear.ramassage.securite;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibellesAuditTest {

    @Test
    void uneRouteConnueDonneUneVraiePhrase() {
        assertEquals("Démarrage d'une tournée", LibellesAudit.libelle("POST", "/api/tournees/{id}/demarrer"));
        assertEquals("Suppression d'un bac", LibellesAudit.libelle("DELETE", "/api/bacs/{id}"));
    }

    @Test
    void uneRouteInconnueGardeSaMethodeEtSonModele() {
        assertEquals("POST /api/nouveau/{id}", LibellesAudit.libelle("POST", "/api/nouveau/{id}"));
    }

    @Test
    void lAbsenceDeModeleNeLevePasDException() {
        assertEquals("POST null", LibellesAudit.libelle("POST", null));
    }
}
