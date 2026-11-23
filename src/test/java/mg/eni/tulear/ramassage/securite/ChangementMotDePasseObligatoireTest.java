package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Un compte au mot de passe imposé ne peut qu'en choisir un nouveau, lire son profil et se déconnecter. */
@WebMvcTest(controllers = {AuthController.class, BacController.class})
@Import(SecurityConfig.class)
class ChangementMotDePasseObligatoireTest {

    @Autowired MockMvc mvc;
    @MockBean UtilisateurService utilisateurService;
    @MockBean ConsultationService consultation;
    @MockBean ParcService parcService;
    @MockBean UserDetailsService userDetailsService;

    private static UtilisateurDetails compte(boolean mdpAChanger) {
        return new UtilisateurDetails(3L, "Rasoa", "Paul", "paul@tulear.mg", "h", Role.CHAUFFEUR, true, mdpAChanger);
    }

    @Test
    void lApiEstFermeeTantQueLeMotDePasseNEstPasChange() throws Exception {
        mvc.perform(get("/api/bacs").with(user(compte(true))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("nouveau mot de passe")));
        verifyNoInteractions(consultation);
    }

    @Test
    void lesAutresComptesNeSontPasRestreints() throws Exception {
        mvc.perform(get("/api/bacs").with(user(compte(false)))).andExpect(status().isOk());
    }

    @Test
    void moiResteAccessibleEtIndiqueLObligation() throws Exception {
        mvc.perform(get("/api/auth/moi").with(user(compte(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mdpAChanger").value(true));
    }

    @Test
    void changerSonMotDePasseResteAccessibleEtLeveLaRestrictionDeLaSession() throws Exception {
        UtilisateurDetails moi = compte(true);
        mvc.perform(post("/api/auth/mot-de-passe").with(user(moi)).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ancienMotDePasse\":\"temporaire-1\",\"nouveauMotDePasse\":\"choisi-par-moi-2\"}"))
                .andExpect(status().isOk());
        verify(utilisateurService).changerMotDePasse(3L, "temporaire-1", "choisi-par-moi-2");

        mvc.perform(get("/api/bacs").with(user(moi))).andExpect(status().isOk());
    }

    @Test
    void sedeconnecterResteAccessible() throws Exception {
        mvc.perform(post("/api/auth/deconnexion").with(user(compte(true))).with(csrf()))
                .andExpect(status().isOk());
    }
}
