package mg.eni.tulear.ramassage.config;

import mg.eni.tulear.ramassage.planification.PlanificateurTournee;
import mg.eni.tulear.ramassage.planification.PlusProcheVoisin;
import mg.eni.tulear.ramassage.planification.PrioriteRemplissage;
import mg.eni.tulear.ramassage.planification.StrategieOptimisation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlanificationConfig {

    /** tournee.strategie = plus-proche-voisin (défaut) ou priorite-remplissage. */
    @Bean
    public StrategieOptimisation strategieOptimisation(@Value("${tournee.strategie:plus-proche-voisin}") String nom) {
        return switch (nom) {
            case "priorite-remplissage" -> new PrioriteRemplissage();
            case "plus-proche-voisin" -> new PlusProcheVoisin();
            default -> throw new IllegalArgumentException("Stratégie inconnue : " + nom);
        };
    }

    @Bean
    public PlanificateurTournee planificateurTournee(StrategieOptimisation strategie) {
        return new PlanificateurTournee(strategie);
    }
}
