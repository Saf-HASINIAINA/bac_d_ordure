package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournee")
public class Tournee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutTournee statut = StatutTournee.PLANIFIEE;

    @Column(name = "distance_km", nullable = false)
    private double distanceKm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "camion_id")
    private Camion camion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chauffeur_id")
    private Chauffeur chauffeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "superviseur_id")
    private Superviseur superviseur;

    @OneToMany(mappedBy = "tournee", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordre")
    private List<Collecte> collectes = new ArrayList<>();

    protected Tournee() { }

    public Tournee(Zone zone, Camion camion, Chauffeur chauffeur) {
        this.zone = zone;
        this.camion = camion;
        this.chauffeur = chauffeur;
    }

    public void ajouterCollecte(Collecte collecte) {
        collectes.add(collecte);
    }

    /** RG8 : PLANIFIEE -> EN_COURS. */
    public void demarrer() {
        if (statut != StatutTournee.PLANIFIEE) throw new IllegalStateException("Tournée non planifiée");
        statut = StatutTournee.EN_COURS;
    }

    /** RG8 : EN_COURS -> TERMINEE. */
    public void terminer() {
        if (statut != StatutTournee.EN_COURS) throw new IllegalStateException("Tournée non démarrée");
        statut = StatutTournee.TERMINEE;
    }

    public double calculerPoidsTotal() {
        return collectes.stream()
                .map(Collecte::getPoidsCollecteKg)
                .filter(p -> p != null)
                .mapToDouble(Double::doubleValue)
                .sum();
    }

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public StatutTournee getStatut() { return statut; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
    public Zone getZone() { return zone; }
    public Camion getCamion() { return camion; }
    public Chauffeur getChauffeur() { return chauffeur; }
    public Superviseur getSuperviseur() { return superviseur; }
    public void setSuperviseur(Superviseur superviseur) { this.superviseur = superviseur; }
    public List<Collecte> getCollectes() { return collectes; }
}
