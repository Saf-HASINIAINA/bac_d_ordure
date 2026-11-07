package mg.eni.tulear.ramassage.config;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.repository.*;
import mg.eni.tulear.ramassage.service.ParametreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Crée des données de démonstration si les tables sont vides (app.init-demo=true). */
@Component
@ConditionalOnProperty(name = "app.init-demo", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final int BACS_PAR_ZONE = 6;

    private final ZoneRepository zoneRepository;
    private final CamionRepository camionRepository;
    private final BacRepository bacRepository;
    private final CapteurRepository capteurRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final ParametreService parametreService;
    private final PasswordEncoder passwordEncoder;
    private final String motDePasseDemo;
    private final boolean reinitialiserMotsDePasse;

    public DataInitializer(ZoneRepository zoneRepository, CamionRepository camionRepository,
                           BacRepository bacRepository, CapteurRepository capteurRepository,
                           UtilisateurRepository utilisateurRepository, ParametreService parametreService,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.demo-password:}") String motDePasseDemo,
                           @Value("${app.reset-demo-passwords:false}") boolean reinitialiserMotsDePasse) {
        this.zoneRepository = zoneRepository;
        this.camionRepository = camionRepository;
        this.bacRepository = bacRepository;
        this.capteurRepository = capteurRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.parametreService = parametreService;
        this.passwordEncoder = passwordEncoder;
        this.motDePasseDemo = motDePasseDemo;
        this.reinitialiserMotsDePasse = reinitialiserMotsDePasse;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (zoneRepository.count() == 0) creerZones();
        if (camionRepository.count() == 0) creerCamions();
        if (bacRepository.count() == 0) creerBacs();
        if (utilisateurRepository.count() == 0) creerUtilisateurs();
        else if (reinitialiserMotsDePasse) reinitialiserMotsDePasse();
    }

    private void creerZones() {
        zoneRepository.saveAll(List.of(
                new Zone("Centre-ville", 12, -23.3500, 43.6700),
                new Zone("Betania", 24, -23.3380, 43.6650),
                new Zone("Mahavatse", 24, -23.3600, 43.6850),
                new Zone("Andaboly", 24, -23.3700, 43.6750)));
        log.info("Zones de démonstration créées");
    }

    private void creerCamions() {
        camionRepository.saveAll(List.of(
                new Camion("1234 TAA", 5000, -23.3550, 43.6700),
                new Camion("5678 TAB", 8000, -23.3550, 43.6700)));
        log.info("Camions de démonstration créés");
    }

    private void creerBacs() {
        Seuils seuils = parametreService.chargerSeuils();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int numero = 1;
        for (Zone zone : zoneRepository.findAll()) {
            for (int i = 0; i < BACS_PAR_ZONE; i++) {
                double lat = zone.getLatitudeCentre() + rnd.nextDouble(-0.004, 0.004);
                double lon = zone.getLongitudeCentre() + rnd.nextDouble(-0.004, 0.004);
                Bac bac = new Bac("BAC-%03d".formatted(numero++), zone, lat, lon, 240, TypeDechet.MENAGER);
                bac.mettreAJourNiveau(rnd.nextDouble(0, 60), seuils);
                bacRepository.save(bac);
                for (TypeCapteur type : TypeCapteur.values()) {
                    capteurRepository.save(CapteurFactory.creer(type, bac));
                }
            }
        }
        log.info("Bacs et capteurs de démonstration créés");
    }

    private static final List<String> EMAILS_DEMO = List.of("admin@tulear.mg", "superviseur@tulear.mg", "chauffeur@tulear.mg");

    private String motDePasseDeDemo() {
        if (!motDePasseDemo.isBlank()) return motDePasseDemo;
        String genere = UUID.randomUUID().toString().substring(0, 12);
        log.warn("Mot de passe des comptes de démo (généré, affiché une seule fois) : {}", genere);
        return genere;
    }

    private void creerUtilisateurs() {
        String hash = passwordEncoder.encode(motDePasseDeDemo());
        utilisateurRepository.saveAll(List.of(
                new Administrateur("Admin", "Système", EMAILS_DEMO.get(0), hash),
                new Superviseur("Rakoto", "Jean", EMAILS_DEMO.get(1), hash),
                new Chauffeur("Rasoa", "Paul", EMAILS_DEMO.get(2), hash, "PERMIS-0001")));
        log.info("Comptes de démo créés : {}", EMAILS_DEMO);
    }

    /** Mot de passe de démo oublié : --app.init-demo=true --app.reset-demo-passwords=true [--app.demo-password=...] */
    private void reinitialiserMotsDePasse() {
        String hash = passwordEncoder.encode(motDePasseDeDemo());
        EMAILS_DEMO.forEach(email -> utilisateurRepository.findByEmailIgnoreCase(email)
                .ifPresent(u -> u.changerMotDePasse(hash)));
        log.info("Mots de passe des comptes de démo réinitialisés");
    }
}
