package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.Role;

/** Classe abstraite : stratégie SINGLE_TABLE, discriminant = colonne role. */
@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role")
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "mot_de_passe_hache", nullable = false)
    private String motDePasseHache;

    @Column(nullable = false)
    private boolean actif = true;

    protected Utilisateur() { }

    protected Utilisateur(String nom, String prenom, String email, String motDePasseHache) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasseHache = motDePasseHache;
    }

    public abstract Role getRole();

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public String getEmail() { return email; }
    public String getMotDePasseHache() { return motDePasseHache; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }
    public void changerMotDePasse(String motDePasseHache) { this.motDePasseHache = motDePasseHache; }
}
