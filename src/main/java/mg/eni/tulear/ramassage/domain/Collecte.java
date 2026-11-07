package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "collecte")
public class Collecte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournee_id")
    private Tournee tournee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bac_id")
    private Bac bac;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "poids_collecte_kg")
    private Double poidsCollecteKg;

    @Column(name = "heure_collecte")
    private Instant heureCollecte;

    @Column(nullable = false)
    private boolean effectuee;

    protected Collecte() { }

    public Collecte(Tournee tournee, Bac bac, int ordre) {
        this.tournee = tournee;
        this.bac = bac;
        this.ordre = ordre;
    }

    /** RG7 : enregistre le poids collecté et vide le bac. */
    public void confirmer(double poidsKg) {
        this.poidsCollecteKg = poidsKg;
        this.heureCollecte = Instant.now();
        this.effectuee = true;
        bac.vider();
    }

    public Long getId() { return id; }
    public Tournee getTournee() { return tournee; }
    public Bac getBac() { return bac; }
    public int getOrdre() { return ordre; }
    public Double getPoidsCollecteKg() { return poidsCollecteKg; }
    public Instant getHeureCollecte() { return heureCollecte; }
    public boolean isEffectuee() { return effectuee; }
}
