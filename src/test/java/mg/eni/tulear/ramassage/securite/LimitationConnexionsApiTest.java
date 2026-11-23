package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Limitation des essais de connexion : 3 échecs puis blocage (réglé à 3 pour ces tests). */
@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {"app.securite.max-echecs=3", "app.securite.duree-blocage-minutes=15"})
class LimitationConnexionsApiTest {

    @Autowired MockMvc mvc;
    @Autowired LimiteurConnexions limiteur;
    @MockBean UtilisateurService utilisateurService;
    @MockBean UserDetailsService userDetailsService;

    private final String hash = new BCryptPasswordEncoder(4).encode("bon-mdp-123");

    @BeforeEach
    void init() {
        for (String email : new String[]{"paul@tulear.mg", "autre@tulear.mg", "inconnu@tulear.mg"}) {
            limiteur.enregistrerSucces(email);
        }
        when(userDetailsService.loadUserByUsername("paul@tulear.mg"))
                .thenReturn(new UtilisateurDetails(3L, "Rasoa", "Paul", "paul@tulear.mg", hash, Role.CHAUFFEUR, true));
        when(userDetailsService.loadUserByUsername("autre@tulear.mg"))
                .thenReturn(new UtilisateurDetails(4L, "Autre", "Un", "autre@tulear.mg", hash, Role.CHAUFFEUR, true));
        when(userDetailsService.loadUserByUsername("inconnu@tulear.mg"))
                .thenThrow(new UsernameNotFoundException("inconnu"));
    }

    private MockHttpServletRequestBuilder connexion(String email, String mdp) {
        return post("/api/auth/connexion").with(csrf()).param("email", email).param("motDePasse", mdp);
    }

    private void echouer(String email, int fois) throws Exception {
        for (int i = 0; i < fois; i++) {
            mvc.perform(connexion(email, "mauvais-mdp")).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void apresTroisEchecsMemeLeBonMotDePasseEstRefuseEn429() throws Exception {
        echouer("paul@tulear.mg", 3);
        mvc.perform(connexion("paul@tulear.mg", "bon-mdp-123"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.message").value(containsString("Trop de tentatives")));
    }

    @Test
    void deuxEchecsPuisLeBonMotDePassePasse() throws Exception {
        echouer("paul@tulear.mg", 2);
        mvc.perform(connexion("paul@tulear.mg", "bon-mdp-123")).andExpect(status().isOk());
    }

    @Test
    void uneConnexionReussieRemetLeCompteurAZero() throws Exception {
        echouer("paul@tulear.mg", 2);
        mvc.perform(connexion("paul@tulear.mg", "bon-mdp-123")).andExpect(status().isOk());
        echouer("paul@tulear.mg", 2);
        mvc.perform(connexion("paul@tulear.mg", "bon-mdp-123")).andExpect(status().isOk());
    }

    @Test
    void leBlocageNeTouchePasLesAutresComptes() throws Exception {
        echouer("paul@tulear.mg", 3);
        mvc.perform(connexion("autre@tulear.mg", "bon-mdp-123")).andExpect(status().isOk());
    }

    @Test
    void uneAdresseInconnueEstBloqueeCommeUneAutrePourNePasRevelerLesComptes() throws Exception {
        echouer("inconnu@tulear.mg", 3);
        mvc.perform(connexion("inconnu@tulear.mg", "n-importe-quoi")).andExpect(status().isTooManyRequests());
    }
}
