package mg.eni.tulear.ramassage.securite;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import java.util.function.Supplier;

/**
 * Protection CSRF pour une page JavaScript : le jeton est déposé dans le cookie XSRF-TOKEN
 * à chaque requête, et le script le renvoie dans l'en-tête X-XSRF-TOKEN (schéma recommandé par Spring Security).
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        xor.handle(request, response, csrfToken);
        csrfToken.get(); // force l'écriture du cookie (le jeton est chargé paresseusement)
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String enTete = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(enTete) ? plain : xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
