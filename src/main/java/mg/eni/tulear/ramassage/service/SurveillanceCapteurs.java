package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Capteur;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.repository.CapteurRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** RG6 : lève une alerte CAPTEUR_PANNE quand un capteur actif n'a plus envoyé de mesure depuis trop longtemps. */
@Component
@ConditionalOnProperty(name = "surveillance.capteurs.actif", havingValue = "true", matchIfMissing = true)
public class SurveillanceCapteurs {

    private static final Logger log = LoggerFactory.getLogger(SurveillanceCapteurs.class);

    private final CapteurRepository capteurRepository;
    private final ParametreService parametreService;
    private final ServiceAlerte serviceAlerte;

    public SurveillanceCapteurs(CapteurRepository capteurRepository, ParametreService parametreService,
                                ServiceAlerte serviceAlerte) {
        this.capteurRepository = capteurRepository;
        this.parametreService = parametreService;
        this.serviceAlerte = serviceAlerte;
    }

    /** Le délai initial laisse à la simulation le temps d'envoyer une première série de mesures. */
    @Scheduled(initialDelayString = "${surveillance.delai-initial-ms:90000}",
            fixedDelayString = "${surveillance.intervalle-ms:60000}")
    @Transactional
    public void surveiller() {
        int enPanne = verifier(Instant.now(), parametreService.chargerSeuils());
        if (enPanne > 0) {
            log.warn("{} capteur(s) sans mesure depuis plus de la limite autorisée", enPanne);
        }
    }

    /**
     * @return le nombre de capteurs jugés en panne. Un capteur qui n'a jamais envoyé de mesure
     *         (derniereMesure absente) est ignoré : on ne peut pas savoir depuis quand il est muet.
     */
    int verifier(Instant maintenant, Seuils seuils) {
        Duration delai = Duration.ofMinutes(seuils.delaiCapteurPanneMin());
        List<Capteur> capteurs = capteurRepository.findActifsAvecBac();
        int enPanne = 0;
        for (Capteur capteur : capteurs) {
            Instant derniere = capteur.getDerniereMesure();
            if (derniere == null) continue;
            Duration silence = Duration.between(derniere, maintenant);
            if (silence.compareTo(delai) > 0) {
                serviceAlerte.signalerCapteurEnPanne(capteur.getBac(), capteur.getType(), silence.toMinutes());
                enPanne++;
            }
        }
        return enPanne;
    }
}
