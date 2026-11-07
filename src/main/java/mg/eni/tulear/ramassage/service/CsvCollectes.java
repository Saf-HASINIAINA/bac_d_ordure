package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Collecte;
import mg.eni.tulear.ramassage.domain.Tournee;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Export CSV des vidanges, prévu pour Excel en français : séparateur « ; », virgule décimale, BOM UTF-8.
 * Les cellules qui commenceraient par = + - @ sont neutralisées (injection de formules).
 */
final class CsvCollectes {

    static final String BOM = "﻿";
    private static final String EN_TETE =
            "Date;Heure;Tournée;Zone;Bac;Chauffeur;Poids collecté (kg);Remplissage à la collecte (%)";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm:ss");

    private CsvCollectes() { }

    static String generer(List<Collecte> collectes, ZoneId fuseau) {
        StringBuilder csv = new StringBuilder(BOM).append(EN_TETE).append("\r\n");
        for (Collecte c : collectes) {
            Tournee t = c.getTournee();
            Bac bac = c.getBac();
            ZonedDateTime heure = c.getHeureCollecte().atZone(fuseau);
            csv.append(String.join(";",
                    cellule(DATE.format(heure)),
                    cellule(HEURE.format(heure)),
                    cellule("n°" + t.getId()),
                    cellule(bac.getZone().getNom()),
                    cellule(bac.getCode()),
                    cellule(t.getChauffeur().getPrenom() + " " + t.getChauffeur().getNom()),
                    nombre(c.getPoidsCollecteKg()),
                    nombre(RapportService.remplissagePourcent(c))))
                    .append("\r\n");
        }
        return csv.toString();
    }

    static String nombre(double valeur) {
        return String.format(Locale.FRANCE, "%.1f", valeur).replace(' ', ' ').replace(' ', ' ');
    }

    static String cellule(String texte) {
        String valeur = texte == null ? "" : texte;
        if (!valeur.isEmpty() && "=+-@\t\r".indexOf(valeur.charAt(0)) >= 0) {
            valeur = "'" + valeur; // neutralise une formule éventuelle
        }
        if (valeur.contains(";") || valeur.contains("\"") || valeur.contains("\n") || valeur.contains("\r")) {
            valeur = "\"" + valeur.replace("\"", "\"\"") + "\"";
        }
        return valeur;
    }
}
