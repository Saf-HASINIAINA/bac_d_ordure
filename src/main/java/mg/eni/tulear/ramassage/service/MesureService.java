package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Capteur;
import mg.eni.tulear.ramassage.domain.Mesure;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;
import mg.eni.tulear.ramassage.repository.MesureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Traite les mesures reçues : enregistrement, mise à jour du bac, évaluation des alertes. */
@Service
public class MesureService {

    private final MesureRepository mesureRepository;
    private final MesureBatchWriter batchWriter;
    private final ServiceAlerte serviceAlerte;

    public MesureService(MesureRepository mesureRepository, MesureBatchWriter batchWriter,
                         ServiceAlerte serviceAlerte) {
        this.mesureRepository = mesureRepository;
        this.batchWriter = batchWriter;
        this.serviceAlerte = serviceAlerte;
    }

    /** Traite une mesure isolée. */
    @Transactional
    public void traiter(Capteur capteur, Mesure mesure, Seuils seuils) {
        mesureRepository.save(mesure);
        appliquer(capteur, mesure, seuils);
    }

    /** Traite un lot de mesures : une seule insertion groupée pour tout le lot. */
    @Transactional
    public void traiterLot(List<Mesure> mesures, Seuils seuils) {
        for (Mesure mesure : mesures) {
            appliquer(mesure.getCapteur(), mesure, seuils);
        }
        batchWriter.inserer(mesures);
    }

    private void appliquer(Capteur capteur, Mesure mesure, Seuils seuils) {
        capteur.setDerniereMesure(mesure.getHorodatage());
        Bac bac = capteur.getBac();
        if (mesure.getType() == TypeMesure.NIVEAU) {
            bac.mettreAJourNiveau(mesure.getValeur(), seuils);
        }
        serviceAlerte.evaluerMesure(bac, mesure, seuils);
    }
}
