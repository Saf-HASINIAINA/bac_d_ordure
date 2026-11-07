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

import java.util.ArrayList;
import java.util.List;

/** Fait « battre le cœur » de la simulation : chaque tick, tous les capteurs actifs mesurent et transmettent. */
@Component
@ConditionalOnProperty(name = "simulation.actif", havingValue = "true", matchIfMissing = true)
public class MoteurSimulation {

    private static final Logger log = LoggerFactory.getLogger(MoteurSimulation.class);

    private final CapteurRepository capteurRepository;
    private final ModuleCommunication communication;
    private final MesureService mesureService;
    private final PannesSimulees pannes;
    private final ParametreService parametreService;
    private final double facteurVitesse;

    public MoteurSimulation(CapteurRepository capteurRepository, ModuleCommunication communication,
                            MesureService mesureService, PannesSimulees pannes, ParametreService parametreService,
                            @Value("${simulation.facteur-vitesse:1.0}") double facteurVitesse) {
        this.capteurRepository = capteurRepository;
        this.communication = communication;
        this.mesureService = mesureService;
        this.pannes = pannes;
        this.parametreService = parametreService;
        this.facteurVitesse = facteurVitesse;
    }

    @Scheduled(initialDelay = 5000, fixedDelayString = "${simulation.intervalle-ms:30000}")
    @Transactional
    public void tick() {
        long debut = System.currentTimeMillis();
        Seuils seuils = parametreService.chargerSeuils();
        List<Capteur> capteurs = capteurRepository.findActifsAvecBac();

        List<Mesure> recues = new ArrayList<>(capteurs.size());
        for (Capteur capteur : capteurs) {
            if (pannes.enPanne(capteur.getId())) {
                continue; // capteur en panne : aucune mesure envoyée
            }
            Mesure mesure = capteur.mesurer(facteurVitesse);
            if (communication.envoyer()) {
                recues.add(mesure);
            }
        }
        mesureService.traiterLot(recues, seuils);

        log.debug("Tick : {}/{} mesures transmises en {} ms",
                recues.size(), capteurs.size(), System.currentTimeMillis() - debut);
    }
}
