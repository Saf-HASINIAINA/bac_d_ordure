package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Mesure;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Sujet du pattern Observer : crée les alertes (RG2, RG5) et prévient les observateurs. */
@Service
public class ServiceAlerte {

    private final AlerteRepository alerteRepository;
    private final List<Observateur> observateurs = new CopyOnWriteArrayList<>();

    /**
     * Alertes connues comme non traitées ("bacId:TYPE"), pour éviter une requête SQL à chaque mesure.
     * Il faut appeler {@link #alerteTraitee} quand une alerte est clôturée.
     */
    private final Set<String> alertesOuvertes = ConcurrentHashMap.newKeySet();

    public ServiceAlerte(AlerteRepository alerteRepository, List<Observateur> observateurs) {
        this.alerteRepository = alerteRepository;
        this.observateurs.addAll(observateurs);
    }

    public void abonner(Observateur observateur) {
        observateurs.add(observateur);
    }

    /** À appeler quand une alerte est traitée (ex. bac vidé), pour qu'une nouvelle puisse être créée. */
    public void alerteTraitee(Long bacId, TypeAlerte type) {
        alertesOuvertes.remove(cle(bacId, type));
    }

    public void evaluerMesure(Bac bac, Mesure mesure, Seuils seuils) {
        switch (mesure.getType()) {
            case NIVEAU -> {
                if (mesure.getValeur() >= seuils.alerteCollecte()) {
                    creerSiAbsente(TypeAlerte.BAC_PLEIN, bac,
                            "Bac %s rempli à %.0f %%".formatted(bac.getCode(), mesure.getValeur()));
                }
            }
            case GAZ -> {
                if (mesure.getValeur() >= seuils.gazIncendie()) {
                    creerSiAbsente(TypeAlerte.RISQUE_INCENDIE, bac,
                            "Risque d'incendie sur le bac %s (valeur %.0f)".formatted(bac.getCode(), mesure.getValeur()));
                }
            }
            default -> { }
        }
    }

    /** RG6 : un capteur n'a plus envoyé de mesure depuis trop longtemps. */
    public void signalerCapteurEnPanne(Bac bac, TypeCapteur type, long minutesSansMesure) {
        creerSiAbsente(TypeAlerte.CAPTEUR_PANNE, bac,
                "Capteur %s du bac %s ne répond plus depuis %d min".formatted(type, bac.getCode(), minutesSansMesure));
    }

    /** Évite de recréer une alerte tant que la précédente du même type n'est pas traitée. */
    private void creerSiAbsente(TypeAlerte type, Bac bac, String message) {
        String cle = cle(bac.getId(), type);
        if (alertesOuvertes.contains(cle)) {
            return;
        }
        if (alerteRepository.existsByBacIdAndTypeAndTraiteeFalse(bac.getId(), type)) {
            alertesOuvertes.add(cle);
            return;
        }
        Alerte alerte = alerteRepository.save(new Alerte(type, message, bac));
        alertesOuvertes.add(cle);
        observateurs.forEach(o -> o.notifier(alerte));
    }

    private static String cle(Long bacId, TypeAlerte type) {
        return bacId + ":" + type;
    }
}
