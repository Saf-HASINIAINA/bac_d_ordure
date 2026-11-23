package mg.eni.tulear.ramassage.securite;

import java.util.Map;

/** Traduit une route (« POST /api/tournees/{id}/demarrer ») en phrase lisible pour le journal d'audit. */
public final class LibellesAudit {

    private static final Map<String, String> LIBELLES = Map.ofEntries(
            Map.entry("POST /api/auth/inscription", "Inscription d'un compte"),
            Map.entry("POST /api/auth/mot-de-passe", "Changement de son mot de passe"),
            Map.entry("POST /api/utilisateurs", "Création d'un compte"),
            Map.entry("POST /api/utilisateurs/{id}/activer", "Activation d'un compte"),
            Map.entry("POST /api/utilisateurs/{id}/desactiver", "Désactivation d'un compte"),
            Map.entry("POST /api/utilisateurs/{id}/reinitialiser-mot-de-passe", "Réinitialisation du mot de passe d'un compte"),
            Map.entry("POST /api/tournees", "Planification d'une tournée"),
            Map.entry("POST /api/tournees/{id}/demarrer", "Démarrage d'une tournée"),
            Map.entry("POST /api/tournees/{id}/collectes/{bacId}/confirmer", "Confirmation de la vidange d'un bac"),
            Map.entry("POST /api/tournees/{id}/incidents", "Signalement d'un incident"),
            Map.entry("POST /api/tournees/{id}/terminer", "Fin d'une tournée"),
            Map.entry("POST /api/tournees/{id}/annuler", "Annulation d'une tournée"),
            Map.entry("POST /api/alertes/{id}/traiter", "Traitement d'une alerte"),
            Map.entry("POST /api/bacs", "Installation d'un bac"),
            Map.entry("PUT /api/bacs/{id}", "Modification d'un bac"),
            Map.entry("DELETE /api/bacs/{id}", "Suppression d'un bac"),
            Map.entry("POST /api/bacs/{id}/hors-service", "Bac mis hors service"),
            Map.entry("POST /api/bacs/{id}/remise-en-service", "Bac remis en service"),
            Map.entry("POST /api/zones", "Création d'une zone"),
            Map.entry("PUT /api/zones/{id}", "Modification d'une zone"),
            Map.entry("DELETE /api/zones/{id}", "Suppression d'une zone"),
            Map.entry("POST /api/camions", "Ajout d'un camion"),
            Map.entry("PUT /api/camions/{id}", "Modification d'un camion"),
            Map.entry("DELETE /api/camions/{id}", "Suppression d'un camion"),
            Map.entry("POST /api/camions/{id}/panne", "Camion déclaré en panne"),
            Map.entry("POST /api/camions/{id}/disponible", "Camion remis disponible"));

    public static final String CONNEXION = "Connexion";
    public static final String ECHEC_CONNEXION = "Échec de connexion";
    public static final String DECONNEXION = "Déconnexion";

    private LibellesAudit() { }

    /** @param modele la route avec ses variables ({id}), ou null si inconnue */
    public static String libelle(String methode, String modele) {
        return LIBELLES.getOrDefault(methode + " " + modele, methode + " " + modele);
    }
}
