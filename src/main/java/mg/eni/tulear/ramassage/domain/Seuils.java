package mg.eni.tulear.ramassage.domain;

import mg.eni.tulear.ramassage.domain.enums.EtatBac;

/** Seuils métier (RG1, RG2, RG6) chargés depuis la table parametre. */
public record Seuils(double moyen, double plein, double critique,
                     double alerteCollecte, int delaiCapteurPanneMin, double gazIncendie) {

    public static Seuils parDefaut() {
        return new Seuils(40, 70, 90, 80, 30, 300);
    }

    /** RG1 : état d'un bac en fonction de son niveau de remplissage. */
    public EtatBac etatPour(double niveau) {
        if (niveau >= critique) return EtatBac.CRITIQUE;
        if (niveau >= plein) return EtatBac.PLEIN;
        if (niveau >= moyen) return EtatBac.MOYEN;
        return EtatBac.VIDE;
    }
}
