package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;

import java.time.Instant;

@Entity
@Table(name = "bac")
public class Bac {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "zone_id")
    private Zone zone;

    @Column(nullable = false)
    private double latitude;

    @Column(nullable = false)
    private double longitude;

    @Column(name = "capacite_litres", nullable = false)
    private double capaciteLitres;

    @Column(name = "niveau_pourcent", nullable = false)
    private double niveauPourcent;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_dechet", nullable = false)
    private TypeDechet typeDechet = TypeDechet.MENAGER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EtatBac etat = EtatBac.VIDE;

    @Column(name = "date_derniere_collecte")
    private Instant dateDerniereCollecte;

    protected Bac() { }

    public Bac(String code, Zone zone, double latitude, double longitude,
               double capaciteLitres, TypeDechet typeDechet) {
        this.code = code;
        this.zone = zone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.capaciteLitres = capaciteLitres;
        this.typeDechet = typeDechet;
    }

    /** Met à jour le niveau (borné à 0..100) et recalcule l'état selon les seuils (RG1). */
    public void mettreAJourNiveau(double niveau, Seuils seuils) {
        this.niveauPourcent = Math.max(0, Math.min(100, niveau));
        if (etat != EtatBac.HORS_SERVICE) {
            this.etat = seuils.etatPour(this.niveauPourcent);
        }
    }

    public boolean estACollecter(double seuilPourcent) {
        return etat != EtatBac.HORS_SERVICE && niveauPourcent >= seuilPourcent;
    }

    /** RG7 : la vidange remet le niveau à 0. */
    public void vider() {
        this.niveauPourcent = 0;
        if (etat != EtatBac.HORS_SERVICE) {
            this.etat = EtatBac.VIDE;
        }
        this.dateDerniereCollecte = Instant.now();
    }

    public void setEtat(EtatBac etat) { this.etat = etat; }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Zone getZone() { return zone; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getCapaciteLitres() { return capaciteLitres; }
    public double getNiveauPourcent() { return niveauPourcent; }
    public TypeDechet getTypeDechet() { return typeDechet; }
    public EtatBac getEtat() { return etat; }
    public Instant getDateDerniereCollecte() { return dateDerniereCollecte; }
}
