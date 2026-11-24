package mg.eni.tulear.ramassage.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Description de l'API pour la documentation interactive (/swagger-ui.html, réservée aux administrateurs). */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ramassageOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Ramassage automatique de bacs - Commune Urbaine de Tuléar")
                .version("1.0")
                .description("API REST de l'application. Authentification par session (cookie) : connectez-vous d'abord "
                        + "sur /login.html. Les requêtes de lecture (GET) s'essaient directement ; les modifications exigent "
                        + "l'en-tête X-XSRF-TOKEN (protection CSRF) que cette page n'ajoute pas."));
    }
}
