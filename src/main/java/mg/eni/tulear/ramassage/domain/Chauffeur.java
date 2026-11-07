package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.Role;

@Entity
@DiscriminatorValue("CHAUFFEUR")
public class Chauffeur extends Utilisateur {

    @Column(name = "numero_permis")
    private String numeroPermis;

    protected Chauffeur() { }

    public Chauffeur(String nom, String prenom, String email, String motDePasseHache, String numeroPermis) {
        super(nom, prenom, email, motDePasseHache);
        this.numeroPermis = numeroPermis;
    }

    @Override
    public Role getRole() { return Role.CHAUFFEUR; }

    public String getNumeroPermis() { return numeroPermis; }
}
