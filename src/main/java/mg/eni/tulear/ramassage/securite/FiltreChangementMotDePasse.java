package mg.eni.tulear.ramassage.securite;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.eni.tulear.ramassage.api.dto.ErreurApi;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Un compte dont le mot de passe a été fixé par un administrateur (ou la commande de secours) ne peut rien faire d'autre
 * que changer ce mot de passe, se déconnecter et lire son profil : toute autre requête de l'API reçoit 403.
 * La restriction est côté serveur ; la redirection du navigateur n'est qu'une commodité.
 */
public class FiltreChangementMotDePasse extends OncePerRequestFilter {

    /** Chemins restant ouverts : changer le mot de passe, savoir qui on est, se déconnecter. */
    private static final Set<String> AUTORISES = Set.of("/api/auth/mot-de-passe", "/api/auth/moi", "/api/auth/deconnexion");

    private final ObjectMapper mapper;

    public FiltreChangementMotDePasse(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chaine)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (req.getRequestURI().startsWith("/api/") && !AUTORISES.contains(req.getRequestURI())
                && auth != null && auth.getPrincipal() instanceof UtilisateurDetails u && u.isMdpAChanger()) {
            res.setStatus(403);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.setCharacterEncoding(StandardCharsets.UTF_8.name());
            res.getWriter().write(mapper.writeValueAsString(
                    ErreurApi.de(403, "Vous devez d'abord choisir un nouveau mot de passe.")));
            return;
        }
        chaine.doFilter(req, res);
    }
}
