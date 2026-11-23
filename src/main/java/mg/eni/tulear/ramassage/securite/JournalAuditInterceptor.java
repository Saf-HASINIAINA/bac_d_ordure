package mg.eni.tulear.ramassage.securite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import mg.eni.tulear.ramassage.service.JournalAuditService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Set;

/**
 * Inscrit au journal d'audit chaque modification (POST, PUT, DELETE) de l'API qui a réussi.
 * Le corps de la requête n'est jamais lu : aucun mot de passe ne peut se retrouver dans le journal.
 * Les connexions et déconnexions sont inscrites par {@link SecurityConfig}.
 */
public class JournalAuditInterceptor implements HandlerInterceptor {

    private static final Set<String> METHODES_MODIFIANTES = Set.of("POST", "PUT", "PATCH", "DELETE");

    private final ObjectProvider<JournalAuditService> journal;

    public JournalAuditInterceptor(ObjectProvider<JournalAuditService> journal) {
        this.journal = journal;
    }

    @Override
    public void afterCompletion(HttpServletRequest requete, HttpServletResponse reponse, Object handler, Exception ex) {
        if (!METHODES_MODIFIANTES.contains(requete.getMethod())) return;
        int statut = reponse.getStatus();
        if (statut < 200 || statut >= 300) return; // on journalise ce qui a réellement eu lieu
        JournalAuditService service = journal.getIfAvailable();
        if (service == null) return;

        String modele = (String) requete.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long id = null;
        String email = null;
        if (auth != null && auth.getPrincipal() instanceof UtilisateurDetails u) {
            id = u.getId();
            email = u.getUsername();
        }
        service.enregistrer(id, email, LibellesAudit.libelle(requete.getMethod(), modele),
                requete.getMethod(), requete.getRequestURI(), statut, requete.getRemoteAddr());
    }
}
