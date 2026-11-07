package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "parametre")
public class Parametre {

    @Id
    private String cle;

    @Column(nullable = false)
    private double valeur;

    private String description;

    protected Parametre() { }

    public String getCle() { return cle; }
    public double getValeur() { return valeur; }
    public void setValeur(double valeur) { this.valeur = valeur; }
    public String getDescription() { return description; }
}
