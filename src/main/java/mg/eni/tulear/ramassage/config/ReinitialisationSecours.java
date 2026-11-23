package mg.eni.tulear.ramassage.config;

import mg.eni.tulear.ramassage.service.GenerateurMotDePasse;
import mg.eni.tulear.ramassage.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

/**
 * Commande de secours pour qui a accès au serveur (utile si le dernier administrateur oublie son mot de passe) :
 * {@code mvn spring-boot:run -Dspring-boot.run.arguments="--app.reset-password-email=admin@tulear.mg"}
 * puis, au choix, {@code --app.new-password=...}; sans cela un mot de passe est généré et affiché dans la console.
 * L'application s'arrête une fois le compte réinitialisé.
 */
@Component
@ConditionalOnProperty(name = "app.reset-password-email")
public class ReinitialisationSecours implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReinitialisationSecours.class);

    private final UtilisateurService utilisateurService;
    private final GenerateurMotDePasse generateur;
    private final ConfigurableApplicationContext contexte;
    private final String email;
    private final String nouveauMotDePasse;

    public ReinitialisationSecours(UtilisateurService utilisateurService, GenerateurMotDePasse generateur,
                                   ConfigurableApplicationContext contexte,
                                   @Value("${app.reset-password-email}") String email,
                                   @Value("${app.new-password:}") String nouveauMotDePasse) {
        this.utilisateurService = utilisateurService;
        this.generateur = generateur;
        this.contexte = contexte;
        this.email = email;
        this.nouveauMotDePasse = nouveauMotDePasse;
    }

    @Override
    public void run(String... args) {
        int sortie = 0;
        try {
            boolean genere = nouveauMotDePasse.isBlank();
            String motDePasse = genere ? generateur.generer() : nouveauMotDePasse;
            if (motDePasse.length() < 8 || motDePasse.length() > 72) {
                throw new IllegalArgumentException("Le mot de passe doit contenir entre 8 et 72 caractères");
            }
            utilisateurService.reinitialiserParEmail(email, motDePasse);
            log.info("Mot de passe de {} réinitialisé.", email);
            if (genere) log.warn("Mot de passe généré (affiché une seule fois) : {}", motDePasse);
        } catch (RuntimeException e) {
            log.error("Réinitialisation impossible : {}", e.getMessage());
            sortie = 1;
        }
        final int code = sortie;
        System.exit(SpringApplication.exit(contexte, () -> code));
    }
}
