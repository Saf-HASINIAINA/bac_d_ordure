package mg.eni.tulear.ramassage.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GenerateurMotDePasseTest {

    private final GenerateurMotDePasse generateur = new GenerateurMotDePasse();

    @Test
    void respecteLaLongueurEtLAlphabetSansCaracteresAmbigus() {
        for (int i = 0; i < 200; i++) {
            String mdp = generateur.generer();
            assertEquals(GenerateurMotDePasse.LONGUEUR, mdp.length());
            assertTrue(mdp.chars().allMatch(c -> GenerateurMotDePasse.ALPHABET.indexOf(c) >= 0), mdp);
            assertTrue(mdp.chars().noneMatch(c -> "0O1lI".indexOf(c) >= 0), "caractère ambigu dans " + mdp);
        }
    }

    @Test
    void accepteParLaValidationDesMotsDePasse() {
        assertTrue(generateur.generer().length() >= 8 && generateur.generer().length() <= 72);
    }

    @Test
    void ilsSontTousDifferents() {
        Set<String> vus = new HashSet<>();
        for (int i = 0; i < 500; i++) assertTrue(vus.add(generateur.generer()), "doublon généré");
    }
}
