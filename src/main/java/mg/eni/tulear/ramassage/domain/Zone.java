package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "zone")
public class Zone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nom;

    @Column(name = "frequence_collecte_heures", nullable = false)
    private int frequenceCollecteHeures = 24;

    @Column(name = "latitude_centre")
    private Double latitudeCentre;

    @Column(name = "longitude_centre")
    private Double longitudeCentre;

    protected Zone() { }

    public Zone(String nom, int frequenceCollecteHeures, double latitudeCentre, double longitudeCentre) {
        this.nom = nom;
        this.frequenceCollecteHeures = frequenceCollecteHeures;
        this.latitudeCentre = latitudeCentre;
        this.longitudeCentre = longitudeCentre;
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public int getFrequenceCollecteHeures() { return frequenceCollecteHeures; }
    public Double getLatitudeCentre() { return latitudeCentre; }
    public Double getLongitudeCentre() { return longitudeCentre; }
}
