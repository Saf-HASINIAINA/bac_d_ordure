package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.*;
import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.config.AuditWebConfig;
import mg.eni.tulear.ramassage.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Le journal d'audit : lecture réservée à l'administrateur, inscription des modifications réussies. */
@WebMvcTest(controllers = {AuditController.class, AuthController.class, UtilisateurController.class, TourneeController.class})
@Import({SecurityConfig.class, AuditWebConfig.class})
class JournalAuditApiTest {

    @Autowired MockMvc mvc;
    @MockBean JournalAuditService journal;
    @MockBean UtilisateurService utilisateurService;
    @MockBean TourneeFacade tournees;
    @MockBean ConsultationService consultation;
    @MockBean UserDetailsService userDetailsService;

    private static RequestPostProcessor connecte(Role role, long id) {
        return user(new UtilisateurDetails(id, "Nom", "Prenom", role.name().toLowerCase() + "@tulear.mg", "h", role, true));
    }

    private static final RequestPostProcessor ADMIN = connecte(Role.ADMIN, 1);
    private static final RequestPostProcessor SUPERVISEUR = connecte(Role.SUPERVISEUR, 2);
    private static final RequestPostProcessor CHAUFFEUR = connecte(Role.CHAUFFEUR, 3);

    @BeforeEach
    void init() {
        when(journal.lister(anyInt(), anyInt())).thenReturn(new PageAuditDto(
                List.of(new EntreeAuditDto(1L, Instant.parse("2026-11-23T10:00:00Z"), 1L, "admin@tulear.mg",
                        "Création d'un compte", "POST", "/api/utilisateurs", 201, "10.0.0.1")), 1, 0, 50, true));
    }

    @Test
    void lAdministrateurLitLeJournal() throws Exception {
        mvc.perform(get("/api/audit").with(ADMIN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.entrees[0].action").value("Création d'un compte"));
    }

    @Test
    void lesAutresRolesEtLesAnonymesNeLisentPasLeJournal() throws Exception {
        mvc.perform(get("/api/audit").with(SUPERVISEUR)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit").with(CHAUFFEUR)).andExpect(status().isForbidden());
        mvc.perform(get("/api/audit")).andExpect(status().isUnauthorized());
        verify(journal, never()).lister(anyInt(), anyInt());
    }

    @Test
    void lesParametresDePageSontTransmis() throws Exception {
        mvc.perform(get("/api/audit?page=2&taille=10").with(ADMIN)).andExpect(status().isOk());
        verify(journal).lister(2, 10);
    }

    @Test
    void uneModificationReussieEstJournaliseeAvecLUtilisateurEtUnLibelle() throws Exception {
        when(utilisateurService.reinitialiserMotDePasse(9L, 1L)).thenReturn("Temporaire23");
        mvc.perform(post("/api/utilisateurs/9/reinitialiser-mot-de-passe").with(ADMIN).with(csrf()))
                .andExpect(status().isOk());

        verify(journal).enregistrer(eq(1L), eq("admin@tulear.mg"), eq("Réinitialisation du mot de passe d'un compte"),
                eq("POST"), eq("/api/utilisateurs/9/reinitialiser-mot-de-passe"), eq(200), anyString());
    }

    @Test
    void leMotDePasseTemporaireNeFigureJamaisDansLeJournal() throws Exception {
        when(utilisateurService.reinitialiserMotDePasse(9L, 1L)).thenReturn("Temporaire23");
        mvc.perform(post("/api/utilisateurs/9/reinitialiser-mot-de-passe").with(ADMIN).with(csrf()));
        verify(journal, never()).enregistrer(any(), any(), contains("Temporaire23"), any(), any(), anyInt(), any());
        verify(journal, never()).enregistrer(any(), any(), any(), any(), contains("Temporaire23"), anyInt(), any());
    }

    @Test
    void uneConsultationNEstPasJournalisee() throws Exception {
        mvc.perform(get("/api/audit").with(ADMIN));
        mvc.perform(get("/api/auth/moi").with(CHAUFFEUR));
        verify(journal, never()).enregistrer(any(), any(), any(), any(), any(), anyInt(), any());
    }

    @Test
    void uneActionRefuseeOuEnEchecNEstPasJournalisee() throws Exception {
        mvc.perform(post("/api/utilisateurs/9/activer").with(CHAUFFEUR).with(csrf())).andExpect(status().isForbidden());
        when(utilisateurService.activer(404L)).thenThrow(new RessourceIntrouvableException("introuvable"));
        mvc.perform(post("/api/utilisateurs/404/activer").with(ADMIN).with(csrf())).andExpect(status().isNotFound());
        verify(journal, never()).enregistrer(any(), any(), any(), any(), any(), anyInt(), any());
    }

    private void compte() {
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(4).encode("bon-mdp-123");
        when(userDetailsService.loadUserByUsername("paul@tulear.mg"))
                .thenReturn(new UtilisateurDetails(3L, "Rasoa", "Paul", "paul@tulear.mg", hash, Role.CHAUFFEUR, true));
    }

    @Test
    void uneConnexionReussieEstJournalisee() throws Exception {
        compte();
        mvc.perform(post("/api/auth/connexion").with(csrf()).param("email", "paul@tulear.mg").param("motDePasse", "bon-mdp-123"))
                .andExpect(status().isOk());
        verify(journal).enregistrer(eq(3L), eq("paul@tulear.mg"), eq("Connexion"), eq("POST"),
                eq("/api/auth/connexion"), eq(200), anyString());
    }

    @Test
    void unEchecDeConnexionEstJournaliseAvecLAdresseSaisieMaisSansLeMotDePasse() throws Exception {
        compte();
        mvc.perform(post("/api/auth/connexion").with(csrf()).param("email", "paul@tulear.mg").param("motDePasse", "mauvais-mdp-9"))
                .andExpect(status().isUnauthorized());
        verify(journal).enregistrer(isNull(), eq("paul@tulear.mg"), eq("Échec de connexion"), eq("POST"),
                eq("/api/auth/connexion"), eq(401), anyString());
        verify(journal, never()).enregistrer(any(), any(), contains("mauvais-mdp-9"), any(), any(), anyInt(), any());
    }

    @Test
    void uneDeconnexionEstJournalisee() throws Exception {
        mvc.perform(post("/api/auth/deconnexion").with(CHAUFFEUR).with(csrf())).andExpect(status().isOk());
        verify(journal).enregistrer(eq(3L), eq("chauffeur@tulear.mg"), eq("Déconnexion"), eq("POST"),
                eq("/api/auth/deconnexion"), eq(200), anyString());
    }

    @Test
    void uneInscriptionAnonymeEstJournaliseeSansUtilisateur() throws Exception {
        mvc.perform(post("/api/auth/inscription").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nom\":\"N\",\"prenom\":\"P\",\"email\":\"n@tulear.mg\",\"motDePasse\":\"motdepasse1\",\"numeroPermis\":\"P-1\"}"))
                .andExpect(status().isCreated());
        verify(journal).enregistrer(isNull(), isNull(), eq("Inscription d'un compte"), eq("POST"),
                eq("/api/auth/inscription"), eq(201), anyString());
    }
}
