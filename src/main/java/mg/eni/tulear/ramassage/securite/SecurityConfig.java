package mg.eni.tulear.ramassage.securite;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import mg.eni.tulear.ramassage.api.dto.ErreurApi;
import mg.eni.tulear.ramassage.api.dto.MessageDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Sécurité de l'application : connexion par session (cookie), protection CSRF, droits par rôle.
 * Les pages publiques sont uniquement login.html et inscription.html ; tout le reste exige une connexion.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain chaineDeSecurite(HttpSecurity http, ObjectMapper mapper) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login.html", "/inscription.html", "/css/**", "/js/**", "/vendor/**",
                        "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/inscription").permitAll()
                // ----- administrateur seul -----
                .requestMatchers("/api/utilisateurs/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/bacs", "/api/zones", "/api/camions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/bacs/*", "/api/zones/*", "/api/camions/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/bacs/*", "/api/zones/*", "/api/camions/*").hasRole("ADMIN")
                // ----- administrateur et superviseur -----
                .requestMatchers("/api/rapports/**").hasAnyRole("ADMIN", "SUPERVISEUR")
                .requestMatchers(HttpMethod.GET, "/api/alertes/**", "/api/statistiques", "/api/zones/**", "/api/chauffeurs")
                        .hasAnyRole("ADMIN", "SUPERVISEUR")
                .requestMatchers(HttpMethod.POST, "/api/bacs/*/hors-service", "/api/bacs/*/remise-en-service",
                        "/api/camions/*/panne", "/api/camions/*/disponible").hasAnyRole("ADMIN", "SUPERVISEUR")
                .requestMatchers(HttpMethod.POST, "/api/tournees", "/api/tournees/*/annuler", "/api/alertes/*/traiter")
                        .hasAnyRole("ADMIN", "SUPERVISEUR")
                // tout utilisateur connecté : carte (bacs, camions), ses tournées, incident, mot de passe.
                // Démarrer / vidanger / terminer / incident : propriétaire vérifié dans TourneeFacade.
                .anyRequest().authenticated())
            .csrf(c -> c
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            // l'API répond 401 ; toute autre requête non connectée est redirigée vers login.html
            .exceptionHandling(e -> e
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        request -> request.getRequestURI().startsWith("/api/"))
                .defaultAuthenticationEntryPointFor(new LoginUrlAuthenticationEntryPoint("/login.html"),
                        request -> true)
                .accessDeniedHandler((req, res, ex) ->
                        ecrire(res, mapper, 403, ErreurApi.de(403, "Accès refusé : droits insuffisants"))))
            .formLogin(f -> f
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/connexion")
                .usernameParameter("email")
                .passwordParameter("motDePasse")
                .successHandler((req, res, auth) -> ecrire(res, mapper, 200, new MessageDto("Connecté")))
                .failureHandler((req, res, ex) ->
                        ecrire(res, mapper, 401, ErreurApi.de(401, messageEchec(ex))))
                .permitAll())
            .logout(l -> l
                .logoutUrl("/api/auth/deconnexion")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> ecrire(res, mapper, 200, new MessageDto("Déconnecté"))))
            .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https://tile.openstreetmap.org; connect-src 'self'; "
                        + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
        return http.build();
    }

    private static String messageEchec(AuthenticationException ex) {
        return ex instanceof DisabledException
                ? "Compte en attente de validation par un administrateur (ou désactivé)."
                : "Email ou mot de passe incorrect.";
    }

    private static void ecrire(HttpServletResponse res, ObjectMapper mapper, int statut, Object corps)
            throws IOException {
        res.setStatus(statut);
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write(mapper.writeValueAsString(corps));
    }
}
