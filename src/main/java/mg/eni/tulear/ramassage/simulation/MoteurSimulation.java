package mg.eni.tulear.ramassage.simulation;

import mg.eni.tulear.ramassage.domain.Capteur;
import mg.eni.tulear.ramassage.domain.Mesure;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.repository.CapteurRepository;
import mg.eni.tulear.ramassage.service.MesureService;
import mg.eni.tulear.ramassage.service.ParametreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Fait « battre le cœur » de la simulation : chaque tick, tous les capteurs actifs mesurent et transmettent. */
@Component
@ConditionalOnProperty(name = "simulation.actif", havingValue = "true", matchIfMissing = true)
public class MoteurSimulation {

    private static final Logger log = LoggerFactory.getLogger(MoteurSimulation.class);

    private final CapteurRepository capteurRepository;
    private final ModuleCommunication communication;
    private final MesureService mesureService;
    private final ParametreService parametreService;
    private final double facteurVitesse;

    public MoteurSimulation(CapteurRepository capteurRepository, ModuleCommunication communication,
                            MesureService mesureService, ParametreService parametreService,
                            @Value("${simulation.facteur-vitesse:1.0}") double facteurVitesse) {
        this.capteurRepository = capteurRepository;
        this.communication = communication;
        this.mesureService = mesureService;
        this.parametreService = parametreService;
        this.facteurVitesse = facteurVitesse;
    }

    @Scheduled(fixedDelayString = "${simulation.intervalle-ms:5000}")
    @Transactional
    public void tick() {
        Seuils seuils = parametreService.chargerSeuils();
        List<Capteur> capteurs = capteurRepository.findByActifTrue();
        int transmises = 0;
        for (Capteur capteur : capteurs) {
            Mesure mesure = capteur.mesurer(facteurVitesse);
            if (communication.envoyer()) {
                mesureService.traiter(capteur, mesure, seuils);
                transmises++;
            }
        }
        log.debug("Tick : {}/{} mesures transmises", transmises, capteurs.size());
    }
}
