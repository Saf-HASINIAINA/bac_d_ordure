package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.AuthController;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La documentation de l'API (Swagger UI) est réservée aux administrateurs. Ce test ne charge pas springdoc :
 * il vérifie seulement la règle d'accès, une route inconnue répondant 404 quand l'accès est accordé.
 */
@WebMvcTest(controllers = AuthController.class)
@Import(SecurityConfig.class)
class DocumentationApiTest {

    private static final String[] CHEMINS = {"/v3/api-docs", "/swagger-ui.html", "/swagger-ui/index.html"};

    @Autowired MockMvc mvc;
    @MockBean UtilisateurService utilisateurService;
    @MockBean UserDetailsService userDetailsService;

    private static RequestPostProcessor connecte(Role role) {
        return user(new UtilisateurDetails(1L, "N", "P", role.name().toLowerCase() + "@tulear.mg", "h", role, true));
    }

    @Test
    void sansConnexionOnEstRenvoyeVersLaPageDeConnexion() throws Exception {
        for (String chemin : CHEMINS) {
            mvc.perform(get(chemin)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login.html"));
        }
    }

    @Test
    void superviseurEtChauffeurSontRefuses() throws Exception {
        for (String chemin : CHEMINS) {
            mvc.perform(get(chemin).with(connecte(Role.SUPERVISEUR))).andExpect(status().isForbidden());
            mvc.perform(get(chemin).with(connecte(Role.CHAUFFEUR))).andExpect(status().isForbidden());
        }
    }

    @Test
    void lAdministrateurPasseLaSecurite() throws Exception {
        for (String chemin : CHEMINS) {
            mvc.perform(get(chemin).with(connecte(Role.ADMIN))).andExpect(status().isNotFound());
        }
    }
}
