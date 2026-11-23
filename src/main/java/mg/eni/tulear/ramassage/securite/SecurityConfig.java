package mg.eni.tulear.ramassage.securite;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.eni.tulear.ramassage.api.dto.ErreurApi;
import mg.eni.tulear.ramassage.api.dto.MessageDto;
import mg.eni.tulear.ramassage.service.JournalAuditService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;

/**
 * Sécurité de l'application : connexion par session (cookie), protection CSRF, droits par rôle.
 * Les pages publiques sont uniquement login.html, inscription.html et mot-de-passe-oublie.html ; tout le reste exige une connexion.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Essais de connexion : {@code app.securite.max-echecs} échecs, puis blocage de {@code app.securite.duree-blocage-minutes}. */
    @Bean
    public LimiteurConnexions limiteurConnexions(
            @Value("${app.securite.max-echecs:5}") int maxEchecs,
            @Value("${app.securite.duree-blocage-minutes:15}") long dureeBlocageMinutes) {
        return new LimiteurConnexions(maxEchecs, Duration.ofMinutes(dureeBlocageMinutes), Clock.systemUTC());
    }

    @Bean
    public SecurityFilterChain chaineDeSecurite(HttpSecurity http, ObjectMapper mapper,
                                                LimiteurConnexions limiteur,
                                                ObjectProvider<JournalAuditService> audit) throws Exception {
        http
            .addFilterBefore(new FiltreLimitationConnexions(limiteur, mapper), UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login.html", "/inscription.html", "/mot-de-passe-oublie.html", "/css/**", "/js/**", "/vendor/**",
                        "/favicon.ico", "/error").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/inscription").permitAll()
                // ----- administrateur seul -----
                .requestMatchers("/api/utilisateurs/**", "/api/audit/**").hasRole("ADMIN")
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
                .successHandler((req, res, auth) -> {
                    limiteur.enregistrerSucces(req.getParameter("email"));
                    tracer(audit, req, auth, LibellesAudit.CONNEXION, 200);
                    ecrire(res, mapper, 200, new MessageDto("Connecté"));
                })
                .failureHandler((req, res, ex) -> {
                    // un compte inactif a fourni le bon mot de passe : ce n'est pas du devinage
                    if (!(ex instanceof DisabledException)) limiteur.enregistrerEchec(req.getParameter("email"));
                    tracer(audit, req, null, LibellesAudit.ECHEC_CONNEXION, 401);
                    ecrire(res, mapper, 401, ErreurApi.de(401, messageEchec(ex)));
                })
                .permitAll())
            .logout(l -> l
                .logoutUrl("/api/auth/deconnexion")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> {
                    tracer(audit, req, auth, LibellesAudit.DECONNEXION, 200);
                    ecrire(res, mapper, 200, new MessageDto("Déconnecté"));
                }))
            .headers(h -> h.contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https://tile.openstreetmap.org; connect-src 'self'; "
                        + "frame-ancestors 'none'; base-uri 'self'; form-action 'self'")));
        return http.build();
    }

    /** Inscrit une connexion, un échec de connexion ou une déconnexion au journal d'audit. */
    private static void tracer(ObjectProvider<JournalAuditService> audit, HttpServletRequest req,
                               Authentication auth, String action, int statut) {
        JournalAuditService journal = audit.getIfAvailable();
        if (journal == null) return;
        Long id = null;
        String email = null;
        if (auth != null && auth.getPrincipal() instanceof UtilisateurDetails u) {
            id = u.getId();
            email = u.getUsername();
        } else if (req.getParameter("email") != null) { // échec : l'adresse saisie (tronquée), compte inconnu ou non
            String saisie = req.getParameter("email").trim();
            email = saisie.length() > 200 ? saisie.substring(0, 200) : saisie;
        }
        journal.enregistrer(id, email, action, req.getMethod(), req.getRequestURI(), statut, req.getRemoteAddr());
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
