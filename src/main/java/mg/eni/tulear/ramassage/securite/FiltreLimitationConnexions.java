package mg.eni.tulear.ramassage.securite;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.eni.tulear.ramassage.api.dto.ErreurApi;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Répond 429 à une tentative de connexion pour une adresse e-mail temporairement bloquée. */
public class FiltreLimitationConnexions extends OncePerRequestFilter {

    private final LimiteurConnexions limiteur;
    private final ObjectMapper mapper;

    public FiltreLimitationConnexions(LimiteurConnexions limiteur, ObjectMapper mapper) {
        this.limiteur = limiteur;
        this.mapper = mapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest requete) {
        return !("POST".equals(requete.getMethod()) && "/api/auth/connexion".equals(requete.getRequestURI()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        Duration reste = limiteur.blocageRestant(requete.getParameter("email"));
        if (reste.isZero()) {
            chaine.doFilter(requete, reponse);
            return;
        }
        long minutes = Math.max(1, (reste.toSeconds() + 59) / 60);
        reponse.setStatus(429);
        reponse.setHeader("Retry-After", String.valueOf(Math.max(1, reste.toSeconds())));
        reponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        reponse.setCharacterEncoding(StandardCharsets.UTF_8.name());
        reponse.getWriter().write(mapper.writeValueAsString(ErreurApi.de(429,
                "Trop de tentatives de connexion. Réessayez dans " + minutes + " minute" + (minutes > 1 ? "s" : "") + ".")));
    }
}
