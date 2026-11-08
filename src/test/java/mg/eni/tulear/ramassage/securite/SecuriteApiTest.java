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
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BacController.class, TourneeController.class, AlerteController.class,
        ReferentielController.class, AuthController.class, UtilisateurController.class})
@Import(SecurityConfig.class)
class SecuriteApiTest {

    private static final String JSON_PLANIF = "{\"zoneId\":1,\"camionId\":2,\"chauffeurId\":3}";

    @Autowired MockMvc mvc;
    @MockBean ConsultationService consultation;
    @MockBean TourneeFacade tournees;
    @MockBean AlerteService alerteService;
    @MockBean UtilisateurService utilisateurService;
    @MockBean ParcService parcService;
    @MockBean UserDetailsService userDetailsService;

    private static RequestPostProcessor connecte(Role role, long id) {
        return user(new UtilisateurDetails(id, "Nom" + id, "Prenom" + id, role.name().toLowerCase() + "@tulear.mg",
                "hash", role, true));
    }

    private static final RequestPostProcessor ADMIN = connecte(Role.ADMIN, 1);
    private static final RequestPostProcessor SUPERVISEUR = connecte(Role.SUPERVISEUR, 2);
    private static final RequestPostProcessor CHAUFFEUR = connecte(Role.CHAUFFEUR, 3);

    private TourneeDto tournee() {
        return new TourneeDto(5L, LocalDate.of(2026, 11, 7), mg.eni.tulear.ramassage.domain.enums.StatutTournee.PLANIFIEE,
                2.3, 1L, "Centre-ville", 2L, "1234 TAA", 3L, "Paul Rasoa", 0, 30, List.of());
    }

    /* ---------- accès sans connexion ---------- */

    @Test
    void apiSansConnexionRenvoie401EnJson() throws Exception {
        mvc.perform(get("/api/bacs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.statut").doesNotExist()); // corps vide : le client lit le code 401
    }

    @Test
    void laPagePrincipaleSansConnexionRedirigeVersLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login.html")));
    }

    @Test
    void laPagePrincipaleSansConnexionRedirigeAussiUnNavigateur() throws Exception {
        mvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", endsWith("/login.html")));
    }

    @Test
    void apiSansConnexionNeRedirigeJamaisMemeAvecAcceptHtml() throws Exception {
        mvc.perform(get("/api/bacs").accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ecritureSansJetonCsrfEstRefusee() throws Exception {
        mvc.perform(post("/api/tournees").with(SUPERVISEUR).contentType(MediaType.APPLICATION_JSON).content(JSON_PLANIF))
                .andExpect(status().isForbidden());
    }

    /* ---------- droits par rôle ---------- */

    @Test
    void lectureOuverteATousLesRolesConnectes() throws Exception {
        for (RequestPostProcessor qui : List.of(ADMIN, SUPERVISEUR, CHAUFFEUR)) {
            mvc.perform(get("/api/bacs").with(qui)).andExpect(status().isOk());
        }
    }

    @Test
    void unChauffeurNePeutPasPlanifierUneTournee() throws Exception {
        mvc.perform(post("/api/tournees").with(CHAUFFEUR).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_PLANIF))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("droits insuffisants")));
        verifyNoInteractions(tournees);
    }

    @Test
    void unSuperviseurPeutPlanifier() throws Exception {
        when(tournees.planifier(any())).thenReturn(tournee());
        mvc.perform(post("/api/tournees").with(SUPERVISEUR).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(JSON_PLANIF))
                .andExpect(status().isCreated());
    }

    @Test
    void annulerEtTraiterUneAlerteSontInterditsAuChauffeur() throws Exception {
        mvc.perform(post("/api/tournees/5/annuler").with(CHAUFFEUR).with(csrf())).andExpect(status().isForbidden());
        mvc.perform(post("/api/alertes/7/traiter").with(CHAUFFEUR).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(tournees, alerteService);
    }

    @Test
    void unChauffeurPeutDemarrerSaTournee() throws Exception {
        when(tournees.demarrer(eq(5L), any())).thenReturn(tournee());
        mvc.perform(post("/api/tournees/5/demarrer").with(CHAUFFEUR).with(csrf())).andExpect(status().isOk());
    }

    @Test
    void unChauffeurNePeutPasToucherALaTourneeDUnAutre() throws Exception {
        when(tournees.demarrer(eq(5L), any())).thenThrow(new AccessDeniedException("autre chauffeur"));
        mvc.perform(post("/api/tournees/5/demarrer").with(CHAUFFEUR).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statut").value(403));
    }

    @Test
    void gestionDesUtilisateursReserveeAUnAdministrateur() throws Exception {
        when(utilisateurService.lister()).thenReturn(List.of());
        mvc.perform(get("/api/utilisateurs").with(CHAUFFEUR)).andExpect(status().isForbidden());
        mvc.perform(get("/api/utilisateurs").with(SUPERVISEUR)).andExpect(status().isForbidden());
        mvc.perform(get("/api/utilisateurs").with(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void desactiverUnCompteTransmetLIdentiteDeLAdministrateur() throws Exception {
        when(utilisateurService.desactiver(eq(9L), eq(1L)))
                .thenReturn(new UtilisateurDto(9L, "A", "B", "a@b.mg", Role.CHAUFFEUR, false, "P1"));
        mvc.perform(post("/api/utilisateurs/9/desactiver").with(ADMIN).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.actif").value(false));
        verify(utilisateurService).desactiver(9L, 1L);
    }

    @Test
    void moiRenvoieLUtilisateurConnecteSansMotDePasse() throws Exception {
        mvc.perform(get("/api/auth/moi").with(SUPERVISEUR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUPERVISEUR"))
                .andExpect(jsonPath("$.email").value("superviseur@tulear.mg"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    /* ---------- inscription publique ---------- */

    private static final String INSCRIPTION =
            "{\"nom\":\"Rasoa\",\"prenom\":\"Paul\",\"email\":\"paul@tulear.mg\",\"motDePasse\":\"motdepasse1\",\"numeroPermis\":\"P-123\"}";

    @Test
    void inscriptionPubliqueRenvoie201() throws Exception {
        mvc.perform(post("/api/auth/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(containsString("valider")));
        verify(utilisateurService).inscrire(any());
    }

    @Test
    void inscriptionSansJetonCsrfEstRefusee() throws Exception {
        mvc.perform(post("/api/auth/inscription").contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION))
                .andExpect(status().isForbidden());
    }

    @Test
    void inscriptionAvecMotDePasseTropCourtRenvoie400() throws Exception {
        mvc.perform(post("/api/auth/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.replace("motdepasse1", "court")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("motDePasse")));
        verifyNoInteractions(utilisateurService);
    }

    @Test
    void inscriptionAvecEmailInvalideRenvoie400() throws Exception {
        mvc.perform(post("/api/auth/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(INSCRIPTION.replace("paul@tulear.mg", "pas-un-email")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emailDejaUtiliseRenvoie409() throws Exception {
        when(utilisateurService.inscrire(any())).thenThrow(new RegleMetierException("Cet email est déjà utilisé"));
        mvc.perform(post("/api/auth/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(INSCRIPTION))
                .andExpect(status().isConflict());
    }

    /* ---------- connexion ---------- */

    private void compte(boolean actif) {
        String hash = new BCryptPasswordEncoder().encode("motdepasse1");
        when(userDetailsService.loadUserByUsername("paul@tulear.mg"))
                .thenReturn(new UtilisateurDetails(3L, "Rasoa", "Paul", "paul@tulear.mg", hash, Role.CHAUFFEUR, actif));
        when(userDetailsService.loadUserByUsername("inconnu@tulear.mg"))
                .thenThrow(new UsernameNotFoundException("inconnu"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder connexion(String email, String mdp) {
        return post("/api/auth/connexion").with(csrf()).param("email", email).param("motDePasse", mdp);
    }

    @Test
    void connexionReussie() throws Exception {
        compte(true);
        mvc.perform(connexion("paul@tulear.mg", "motdepasse1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connecté"));
    }

    @Test
    void mauvaisMotDePasseRenvoie401() throws Exception {
        compte(true);
        mvc.perform(connexion("paul@tulear.mg", "mauvais"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void emailInconnuDonneLeMemeMessageQueLeMauvaisMotDePasse() throws Exception {
        compte(true);
        mvc.perform(connexion("inconnu@tulear.mg", "motdepasse1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email ou mot de passe incorrect."));
    }

    @Test
    void compteEnAttenteDeValidationNePeutPasSeConnecter() throws Exception {
        compte(false);
        mvc.perform(connexion("paul@tulear.mg", "motdepasse1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("en attente de validation")));
    }

    @Test
    void connexionSansJetonCsrfEstRefusee() throws Exception {
        compte(true);
        mvc.perform(post("/api/auth/connexion").param("email", "paul@tulear.mg").param("motDePasse", "motdepasse1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void deconnexionRenvoie200() throws Exception {
        mvc.perform(post("/api/auth/deconnexion").with(SUPERVISEUR).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Déconnecté"));
    }
}
