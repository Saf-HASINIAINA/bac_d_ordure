package mg.eni.tulear.ramassage.planification;

import mg.eni.tulear.ramassage.domain.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Construit une tournée : sélectionne les bacs à collecter (RG2) sans dépasser la capacité du camion (RG3),
 * en privilégiant les plus remplis (RG5), puis les ordonne avec la stratégie choisie.
 * Classe sans dépendance à la base de données : facile à tester.
 */
public class PlanificateurTournee {

    private StrategieOptimisation strategie;

    public PlanificateurTournee(StrategieOptimisation strategie) {
        this.strategie = strategie;
    }

    public void definirStrategie(StrategieOptimisation strategie) {
        this.strategie = strategie;
    }

    public Optional<Tournee> planifier(Zone zone, List<Bac> candidats, Camion camion,
                                       Chauffeur chauffeur, Position depot, Seuils seuils) {
        double capaciteRestante = camion.getCapaciteKg() - camion.getChargeActuelleKg();

        List<Bac> retenus = new ArrayList<>();
        double poidsTotal = 0;
        List<Bac> parPriorite = candidats.stream()
                .filter(b -> b.estACollecter(seuils.alerteCollecte()))
                .sorted(Comparator.comparingDouble(Bac::getNiveauPourcent).reversed())
                .toList();
        for (Bac bac : parPriorite) {
            double poids = bac.estimerPoidsKg();
            if (poidsTotal + poids <= capaciteRestante) {
                retenus.add(bac);
                poidsTotal += poids;
            }
        }
        if (retenus.isEmpty()) {
            return Optional.empty();
        }

        List<Bac> ordonnes = strategie.calculer(retenus, camion, depot);
        Tournee tournee = new Tournee(zone, camion, chauffeur);
        int ordre = 1;
        for (Bac bac : ordonnes) {
            tournee.ajouterCollecte(new Collecte(tournee, bac, ordre++));
        }
        tournee.setDistanceKm(distanceTotale(depot, ordonnes));
        return Optional.of(tournee);
    }

    /** Trajet complet : dépôt -> bacs dans l'ordre -> retour au dépôt. */
    static double distanceTotale(Position depot, List<Bac> ordre) {
        double total = 0;
        Position courante = depot;
        for (Bac bac : ordre) {
            total += courante.distanceVers(bac.getPosition());
            courante = bac.getPosition();
        }
        return total + courante.distanceVers(depot);
    }
}
