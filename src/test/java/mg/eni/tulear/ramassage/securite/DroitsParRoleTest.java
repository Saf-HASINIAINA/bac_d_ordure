package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.*;
import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vérifie, route par route et rôle par rôle, qui a le droit de quoi.
 * Une route « autorisée » ne doit jamais répondre 403 ; une route « interdite » doit répondre 403.
 */
@WebMvcTest(controllers = {BacController.class, ZoneController.class, CamionController.class, TourneeController.class,
        AlerteController.class, ReferentielController.class, AuthController.class, UtilisateurController.class,
        RapportController.class})
@Import(SecurityConfig.class)
class DroitsParRoleTest {

    private static final Set<Role> TOUS = EnumSet.of(Role.ADMIN, Role.SUPERVISEUR, Role.CHAUFFEUR);
    private static final Set<Role> GESTION = EnumSet.of(Role.ADMIN, Role.SUPERVISEUR);
    private static final Set<Role> ADMIN = EnumSet.of(Role.ADMIN);

    private static final String BAC = "{\"zoneId\":1,\"latitude\":-23.35,\"longitude\":43.67,\"capaciteLitres\":240}";
    private static final String ZONE = "{\"nom\":\"Zone test\",\"frequenceCollecteHeures\":24}";
    private static final String CAMION = "{\"immatriculation\":\"1234 TAA\",\"capaciteKg\":5000}";
    private static final String TOURNEE = "{\"zoneId\":1,\"camionId\":1,\"chauffeurId\":3}";
    private static final String INCIDENT = "{\"description\":\"Bac cassé\"}";
    private static final String MDP = "{\"ancienMotDePasse\":\"ancien-mdp\",\"nouveauMotDePasse\":\"nouveau-mdp-1\"}";
    private static final String COMPTE = "{\"role\":\"SUPERVISEUR\",\"nom\":\"N\",\"prenom\":\"P\",\"email\":\"n@tulear.mg\",\"motDePasse\":\"motdepasse-1\"}";

    private record Route(HttpMethod methode, String url, String corps, Set<Role> autorises) {
        @Override public String toString() { return methode + " " + url; }
    }

    private static final List<Route> ROUTES = List.of(
            // consultation : la carte est visible de tous
            new Route(HttpMethod.GET, "/api/bacs", null, TOUS),
            new Route(HttpMethod.GET, "/api/bacs/1", null, TOUS),
            new Route(HttpMethod.GET, "/api/camions", null, TOUS),
            new Route(HttpMethod.GET, "/api/configuration", null, TOUS),
            new Route(HttpMethod.GET, "/api/tournees", null, TOUS),
            new Route(HttpMethod.GET, "/api/tournees/1", null, TOUS),
            new Route(HttpMethod.GET, "/api/auth/moi", null, TOUS),
            // exploitation : administrateur et superviseur
            new Route(HttpMethod.GET, "/api/zones", null, GESTION),
            new Route(HttpMethod.GET, "/api/chauffeurs", null, GESTION),
            new Route(HttpMethod.GET, "/api/statistiques", null, GESTION),
            new Route(HttpMethod.GET, "/api/alertes", null, GESTION),
            new Route(HttpMethod.POST, "/api/alertes/1/traiter", null, GESTION),
            new Route(HttpMethod.GET, "/api/rapports", null, GESTION),
            new Route(HttpMethod.GET, "/api/rapports/export.csv", null, GESTION),
            new Route(HttpMethod.POST, "/api/tournees", TOURNEE, GESTION),
            new Route(HttpMethod.POST, "/api/tournees/1/annuler", null, GESTION),
            new Route(HttpMethod.POST, "/api/bacs/1/hors-service", null, GESTION),
            new Route(HttpMethod.POST, "/api/bacs/1/remise-en-service", null, GESTION),
            new Route(HttpMethod.POST, "/api/camions/1/panne", null, GESTION),
            new Route(HttpMethod.POST, "/api/camions/1/disponible", null, GESTION),
            // déroulement d'une tournée : tous (le chauffeur seulement sur les siennes, vérifié ailleurs)
            new Route(HttpMethod.POST, "/api/tournees/1/demarrer", null, TOUS),
            new Route(HttpMethod.POST, "/api/tournees/1/collectes/1/confirmer", null, TOUS),
            new Route(HttpMethod.POST, "/api/tournees/1/terminer", null, TOUS),
            new Route(HttpMethod.POST, "/api/tournees/1/incidents", INCIDENT, TOUS),
            new Route(HttpMethod.POST, "/api/auth/mot-de-passe", MDP, TOUS),
            // parc et comptes : administrateur seul
            new Route(HttpMethod.POST, "/api/bacs", BAC, ADMIN),
            new Route(HttpMethod.PUT, "/api/bacs/1", BAC, ADMIN),
            new Route(HttpMethod.DELETE, "/api/bacs/1", null, ADMIN),
            new Route(HttpMethod.POST, "/api/zones", ZONE, ADMIN),
            new Route(HttpMethod.PUT, "/api/zones/1", ZONE, ADMIN),
            new Route(HttpMethod.DELETE, "/api/zones/1", null, ADMIN),
            new Route(HttpMethod.POST, "/api/camions", CAMION, ADMIN),
            new Route(HttpMethod.PUT, "/api/camions/1", CAMION, ADMIN),
            new Route(HttpMethod.DELETE, "/api/camions/1", null, ADMIN),
            new Route(HttpMethod.GET, "/api/utilisateurs", null, ADMIN),
            new Route(HttpMethod.POST, "/api/utilisateurs", COMPTE, ADMIN),
            new Route(HttpMethod.POST, "/api/utilisateurs/1/activer", null, ADMIN),
            new Route(HttpMethod.POST, "/api/utilisateurs/1/desactiver", null, ADMIN));

    @Autowired MockMvc mvc;
    @MockBean ConsultationService consultation;
    @MockBean TourneeFacade tournees;
    @MockBean AlerteService alerteService;
    @MockBean UtilisateurService utilisateurService;
    @MockBean ParcService parc;
    @MockBean RapportService rapportService;
    @MockBean UserDetailsService userDetailsService;

    @org.junit.jupiter.api.BeforeEach
    void preparerLesFauxServices() {
        when(rapportService.exporterCsv(any(), any())).thenReturn("");
        when(rapportService.nomFichier(any(), any())).thenReturn("rapport.csv");
    }

    private int statutPour(Route route, Role role) throws Exception {
        var requete = request(route.methode(), route.url())
                .with(user(new UtilisateurDetails(role == Role.ADMIN ? 1L : role == Role.SUPERVISEUR ? 2L : 3L,
                        "Nom", "Prenom", role.name().toLowerCase() + "@tulear.mg", "h", role, true)))
                .with(csrf());
        if (route.corps() != null) {
            requete.contentType(MediaType.APPLICATION_JSON).content(route.corps());
        }
        return mvc.perform(requete).andReturn().getResponse().getStatus();
    }

    @Test
    void chaqueRouteEstOuverteAuxBonsRolesEtFermeeAuxAutres() throws Exception {
        List<String> erreurs = new ArrayList<>();
        for (Route route : ROUTES) {
            for (Role role : Role.values()) {
                int statut = statutPour(route, role);
                boolean autorise = route.autorises().contains(role);
                if (autorise && statut == 403) erreurs.add(role + " devrait pouvoir faire " + route + " mais reçoit 403");
                if (!autorise && statut != 403) erreurs.add(role + " ne devrait PAS pouvoir faire " + route + " mais reçoit " + statut);
            }
        }
        assertEquals(List.of(), erreurs, "écarts avec la matrice des droits");
    }

    @Test
    void lesRoutesInterditesNAtteignentJamaisLesServices() throws Exception {
        statutPour(new Route(HttpMethod.POST, "/api/bacs", BAC, ADMIN), Role.SUPERVISEUR);
        statutPour(new Route(HttpMethod.POST, "/api/bacs", BAC, ADMIN), Role.CHAUFFEUR);
        statutPour(new Route(HttpMethod.DELETE, "/api/zones/1", null, ADMIN), Role.SUPERVISEUR);
        org.mockito.Mockito.verifyNoInteractions(parc);
    }

    @Test
    void laMatriceCouvreAuMoinsLesTroisRolesSurChaqueFamilleDeRoute() {
        assertTrue(ROUTES.size() >= 35, "la matrice doit rester complète : " + ROUTES.size() + " routes");
    }

    /* ---------- validation des demandes (administrateur) ---------- */

    private int poster(String url, String corps) throws Exception {
        return mvc.perform(request(HttpMethod.POST, url)
                        .with(user(new UtilisateurDetails(1L, "A", "B", "admin@tulear.mg", "h", Role.ADMIN, true)))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(corps))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void lesDemandesInvalidesRenvoient400() throws Exception {
        assertEquals(400, poster("/api/bacs", BAC.replace("-23.35", "-123.35")), "latitude hors limites");
        assertEquals(400, poster("/api/bacs", BAC.replace("43.67", "243.67")), "longitude hors limites");
        assertEquals(400, poster("/api/bacs", BAC.replace("240", "5")), "capacité trop petite");
        assertEquals(400, poster("/api/bacs", BAC.replace("\"zoneId\":1,", "")), "zone manquante");
        assertEquals(400, poster("/api/bacs", BAC.replace("{", "{\"code\":\"a b\",")), "code avec espace");
        assertEquals(400, poster("/api/zones", ZONE.replace("Zone test", " ")), "nom vide");
        assertEquals(400, poster("/api/zones", ZONE.replace("24", "0")), "fréquence nulle");
        assertEquals(400, poster("/api/camions", CAMION.replace("5000", "10")), "capacité trop petite");
        assertEquals(400, poster("/api/camions", CAMION.replace("1234 TAA", "")), "immatriculation vide");
        assertEquals(400, poster("/api/auth/mot-de-passe", MDP.replace("nouveau-mdp-1", "court")), "mot de passe trop court");
        assertEquals(400, poster("/api/tournees/1/incidents", INCIDENT.replace("Bac cassé", "ab")), "description trop courte");
    }

    @Test
    void lesDemandesValidesPassentEtLesReglesMetierDonnent409Ou404() throws Exception {
        when(parc.installerBac(any())).thenThrow(new RegleMetierException("Le code BAC-001 est déjà utilisé par un autre bac"));
        assertEquals(409, poster("/api/bacs", BAC));

        when(parc.creerZone(any())).thenThrow(new RessourceIntrouvableException("Zone introuvable : 1"));
        assertEquals(404, poster("/api/zones", ZONE));

        when(parc.creerCamion(any())).thenReturn(new CamionDto(1L, "1234 TAA", 5000, 0, null, null, null));
        assertEquals(201, poster("/api/camions", CAMION));
    }

    @Test
    void unMauvaisMotDePasseActuelRenvoie400AvecUnMessageClair() throws Exception {
        org.mockito.Mockito.doThrow(new RequeteInvalideException("Le mot de passe actuel est incorrect"))
                .when(utilisateurService).changerMotDePasse(anyLong(), any(), any());
        mvc.perform(request(HttpMethod.POST, "/api/auth/mot-de-passe")
                        .with(user(new UtilisateurDetails(3L, "A", "B", "c@tulear.mg", "h", Role.CHAUFFEUR, true)))
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON).content(MDP))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le mot de passe actuel est incorrect"));
    }
}
