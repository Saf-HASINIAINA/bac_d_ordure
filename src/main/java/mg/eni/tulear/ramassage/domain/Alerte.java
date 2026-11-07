package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.*;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;

import java.time.Instant;

@Entity
@Table(name = "alerte")
public class Alerte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeAlerte type;

    @Column(nullable = false)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bac_id")
    private Bac bac;

    @Column(name = "date_creation", nullable = false)
    private Instant dateCreation = Instant.now();

    @Column(nullable = false)
    private boolean traitee;

    protected Alerte() { }

    public Alerte(TypeAlerte type, String message, Bac bac) {
        this.type = type;
        this.message = message;
        this.bac = bac;
    }

    public void marquerTraitee() { this.traitee = true; }

    public Long getId() { return id; }
    public TypeAlerte getType() { return type; }
    public String getMessage() { return message; }
    public Bac getBac() { return bac; }
    public Instant getDateCreation() { return dateCreation; }
    public boolean isTraitee() { return traitee; }
}
