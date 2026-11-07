package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Parametre;
import mg.eni.tulear.ramassage.domain.Seuils;
import mg.eni.tulear.ramassage.repository.ParametreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParametreService {

    private final ParametreRepository parametreRepository;

    public ParametreService(ParametreRepository parametreRepository) {
        this.parametreRepository = parametreRepository;
    }

    @Transactional(readOnly = true)
    public Seuils chargerSeuils() {
        Map<String, Double> p = parametreRepository.findAll().stream()
                .collect(Collectors.toMap(Parametre::getCle, Parametre::getValeur));
        Seuils d = Seuils.parDefaut();
        return new Seuils(
                p.getOrDefault("seuil_moyen_pct", d.moyen()),
                p.getOrDefault("seuil_plein_pct", d.plein()),
                p.getOrDefault("seuil_critique_pct", d.critique()),
                p.getOrDefault("seuil_alerte_collecte_pct", d.alerteCollecte()),
                p.getOrDefault("delai_capteur_panne_min", (double) d.delaiCapteurPanneMin()).intValue(),
                p.getOrDefault("seuil_gaz_incendie", d.gazIncendie()));
    }
}
