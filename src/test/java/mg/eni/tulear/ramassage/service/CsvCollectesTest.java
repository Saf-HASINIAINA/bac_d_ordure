package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvCollectesTest {

    private static final ZoneId MADAGASCAR = ZoneId.of("Indian/Antananarivo");

    private Collecte collecte(String nomChauffeur, String prenomChauffeur, double poids) {
        Zone zone = new Zone("Centre-ville", 24, 0, 0);
        Bac bac = new Bac("BAC-001", zone, 0, 0, 240, TypeDechet.MENAGER);
        Chauffeur chauffeur = new Chauffeur(nomChauffeur, prenomChauffeur, "c@tulear.mg", "h", "P1");
        Tournee t = new Tournee(zone, new Camion("1234 TAA", 1000, 0, 0), chauffeur);
        ReflectionTestUtils.setField(t, "id", 7L);
        Collecte c = new Collecte(t, bac, 1);
        ReflectionTestUtils.setField(c, "poidsCollecteKg", poids);
        ReflectionTestUtils.setField(c, "heureCollecte", Instant.parse("2026-11-06T22:30:00Z"));
        return c;
    }

    @Test
    void commenceParUnBomUtf8PourExcel() {
        assertTrue(CsvCollectes.generer(List.of(), MADAGASCAR).startsWith("﻿"));
    }

    @Test
    void lEnTeteEtLaLigneUtilisentLePointVirguleEtLaVirguleDecimale() {
        String csv = CsvCollectes.generer(List.of(collecte("Rasoa", "Paul", 18.5)), MADAGASCAR);
        String[] lignes = csv.split("\r\n");

        assertEquals("﻿Date;Heure;Tournée;Zone;Bac;Chauffeur;Poids collecté (kg);Remplissage à la collecte (%)", lignes[0]);
        // 22h30 UTC = 01h30 le lendemain à Madagascar
        assertEquals("2026-11-07;01:30:00;n°7;Centre-ville;BAC-001;Paul Rasoa;18,5;51,4", lignes[1]);
    }

    @Test
    void uneCelluleQuiCommenceParUnOperateurDeFormuleEstNeutralisee() {
        assertEquals("'=SOMME(A1)", CsvCollectes.cellule("=SOMME(A1)"));
        assertEquals("'+33", CsvCollectes.cellule("+33"));
        assertEquals("'-1", CsvCollectes.cellule("-1"));
        assertEquals("'@cmd", CsvCollectes.cellule("@cmd"));
    }

    @Test
    void unNomDeChauffeurMalveillantNeDevientPasUneFormule() {
        String csv = CsvCollectes.generer(List.of(collecte("=HYPERLINK(\"http://evil\")", "Paul", 10)), MADAGASCAR);
        assertFalse(csv.contains(";=HYPERLINK"), "aucune cellule ne doit commencer par =");
        assertFalse(csv.contains(";Paul =HYPERLINK"), "le prénom est devant, la formule au milieu reste du texte");
    }

    @Test
    void lesSeparateursEtGuillemetsSontProtegesParDesGuillemets() {
        assertEquals("\"a;b\"", CsvCollectes.cellule("a;b"));
        assertEquals("\"dit \"\"oui\"\"\"", CsvCollectes.cellule("dit \"oui\""));
        assertEquals("\"deux\nlignes\"", CsvCollectes.cellule("deux\nlignes"));
    }

    @Test
    void uneValeurNulleDonneUneCelluleVide() {
        assertEquals("", CsvCollectes.cellule(null));
    }

    @Test
    void lesNombresUtilisentLaVirguleEtUnSeulChiffreDecimal() {
        assertEquals("1234,6", CsvCollectes.nombre(1234.56).replace(" ", ""));
        assertEquals("0,0", CsvCollectes.nombre(0));
    }
}
