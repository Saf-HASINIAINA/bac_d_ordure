package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.StatutCamion;

@Entity
@Table(name = "camion")
public class Camion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String immatriculation;

    @Column(name = "capacite_kg", nullable = false)
    private double capaciteKg;

    @Column(name = "charge_actuelle_kg", nullable = false)
    private double chargeActuelleKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutCamion statut = StatutCamion.DISPONIBLE;

    private Double latitude;
    private Double longitude;

    protected Camion() { }

    public Camion(String immatriculation, double capaciteKg, double latitude, double longitude) {
        this.immatriculation = immatriculation;
        this.capaciteKg = capaciteKg;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public boolean peutCharger(double kg) {
        return chargeActuelleKg + kg <= capaciteKg;
    }

    public void charger(double kg) {
        if (!peutCharger(kg)) throw new IllegalStateException("Capacité du camion dépassée (RG3)");
        chargeActuelleKg += kg;
    }

    public void decharger() { chargeActuelleKg = 0; }

    public void deplacerVers(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public String getImmatriculation() { return immatriculation; }
    public double getCapaciteKg() { return capaciteKg; }
    public double getChargeActuelleKg() { return chargeActuelleKg; }
    public StatutCamion getStatut() { return statut; }
    public void setStatut(StatutCamion statut) { this.statut = statut; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
