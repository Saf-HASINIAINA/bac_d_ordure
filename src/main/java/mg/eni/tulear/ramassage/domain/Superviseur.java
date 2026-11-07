package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.Role;

@Entity
@DiscriminatorValue("SUPERVISEUR")
public class Superviseur extends Utilisateur {

    protected Superviseur() { }

    public Superviseur(String nom, String prenom, String email, String motDePasseHache) {
        super(nom, prenom, email, motDePasseHache);
    }

    @Override
    public Role getRole() { return Role.SUPERVISEUR; }
}
