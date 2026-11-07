package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Capteur;
import mg.eni.tulear.ramassage.domain.Mesure;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;
import mg.eni.tulear.ramassage.repository.MesureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Traite une mesure reçue : enregistrement, mise à jour du bac, évaluation des alertes. */
@Service
public class MesureService {

    private final MesureRepository mesureRepository;
    private final ServiceAlerte serviceAlerte;

    public MesureService(MesureRepository mesureRepository, ServiceAlerte serviceAlerte) {
        this.mesureRepository = mesureRepository;
        this.serviceAlerte = serviceAlerte;
    }

    @Transactional
    public void traiter(Capteur capteur, Mesure mesure, Seuils seuils) {
        mesureRepository.save(mesure);
        capteur.setDerniereMesure(mesure.getHorodatage());
        Bac bac = capteur.getBac();
        if (mesure.getType() == TypeMesure.NIVEAU) {
            bac.mettreAJourNiveau(mesure.getValeur(), seuils);
        }
        serviceAlerte.evaluerMesure(bac, mesure, seuils);
    }
}
