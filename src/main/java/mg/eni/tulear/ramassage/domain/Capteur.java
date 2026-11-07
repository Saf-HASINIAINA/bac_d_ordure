package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

import java.time.Instant;

/** Capteur simulé : chaque sous-classe produit sa propre mesure (polymorphisme). */
@Entity
@Table(name = "capteur")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class Capteur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bac_id")
    private Bac bac;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "derniere_mesure")
    private Instant derniereMesure;

    protected Capteur() { }

    protected Capteur(Bac bac) { this.bac = bac; }

    public abstract TypeCapteur getType();

    public Mesure mesurer() { return mesurer(1.0); }

    /** @param facteurVitesse accélération du temps simulé (1.0 = temps réel) */
    public abstract Mesure mesurer(double facteurVitesse);

    protected Mesure nouvelleMesure(TypeMesure type, double valeur) {
        return new Mesure(this, type, valeur);
    }

    public Long getId() { return id; }
    public Bac getBac() { return bac; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public Instant getDerniereMesure() { return derniereMesure; }
    public void setDerniereMesure(Instant derniereMesure) { this.derniereMesure = derniereMesure; }
}
