package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;

import java.time.Instant;

@Entity
@Table(name = "mesure")
public class Mesure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capteur_id")
    private Capteur capteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeMesure type;

    @Column(nullable = false)
    private double valeur;

    @Column(nullable = false)
    private Instant horodatage = Instant.now();

    protected Mesure() { }

    public Mesure(Capteur capteur, TypeMesure type, double valeur) {
        this.capteur = capteur;
        this.type = type;
        this.valeur = valeur;
    }

    public Long getId() { return id; }
    public Capteur getCapteur() { return capteur; }
    public TypeMesure getType() { return type; }
    public double getValeur() { return valeur; }
    public Instant getHorodatage() { return horodatage; }
}
