package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Une ligne du journal d'audit : une action faite (ou tentée) par un utilisateur. Jamais modifiée après création. */
@Entity
@Table(name = "journal_audit")
public class EntreeAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_action", nullable = false)
    private Instant dateAction = Instant.now();

    /** Vide pour une action anonyme (inscription, connexion échouée). */
    @Column(name = "utilisateur_id")
    private Long utilisateurId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String methode;

    @Column(nullable = false)
    private String chemin;

    @Column(nullable = false)
    private int statut;

    @Column(name = "adresse_ip")
    private String adresseIp;

    protected EntreeAudit() { }

    public EntreeAudit(Long utilisateurId, String email, String action, String methode, String chemin,
                       int statut, String adresseIp) {
        this.utilisateurId = utilisateurId;
        this.email = email;
        this.action = action;
        this.methode = methode;
        this.chemin = chemin;
        this.statut = statut;
        this.adresseIp = adresseIp;
    }

    public Long getId() { return id; }
    public Instant getDateAction() { return dateAction; }
    public Long getUtilisateurId() { return utilisateurId; }
    public String getEmail() { return email; }
    public String getAction() { return action; }
    public String getMethode() { return methode; }
    public String getChemin() { return chemin; }
    public int getStatut() { return statut; }
    public String getAdresseIp() { return adresseIp; }
}
