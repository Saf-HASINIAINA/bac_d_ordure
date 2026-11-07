package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Mesure;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Sujet du pattern Observer : crée les alertes (RG2, RG5) et prévient les observateurs. */
@Service
public class ServiceAlerte {

    private final AlerteRepository alerteRepository;
    private final List<Observateur> observateurs = new CopyOnWriteArrayList<>();

    public ServiceAlerte(AlerteRepository alerteRepository, List<Observateur> observateurs) {
        this.alerteRepository = alerteRepository;
        this.observateurs.addAll(observateurs);
    }

    public void abonner(Observateur observateur) {
        observateurs.add(observateur);
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

    /** Évite de recréer une alerte tant que la précédente du même type n'est pas traitée. */
    private void creerSiAbsente(TypeAlerte type, Bac bac, String message) {
        if (alerteRepository.existsByBacIdAndTypeAndTraiteeFalse(bac.getId(), type)) {
            return;
        }
        Alerte alerte = alerteRepository.save(new Alerte(type, message, bac));
        observateurs.forEach(o -> o.notifier(alerte));
    }
}
