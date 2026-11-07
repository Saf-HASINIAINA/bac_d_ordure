package mg.eni.tulear.ramassage.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import mg.eni.tulear.ramassage.domain.enums.Role;

@Entity
@DiscriminatorValue("ADMIN")
public class Administrateur extends Utilisateur {

    protected Administrateur() { }

    public Administrateur(String nom, String prenom, String email, String motDePasseHache) {
        super(nom, prenom, email, motDePasseHache);
    }

    @Override
    public Role getRole() { return Role.ADMIN; }
}
