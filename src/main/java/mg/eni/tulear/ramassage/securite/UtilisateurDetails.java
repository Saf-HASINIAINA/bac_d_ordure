package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.domain.Utilisateur;
import mg.eni.tulear.ramassage.domain.enums.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Utilisateur connecté, tel que Spring Security le voit (conservé dans la session). */
public class UtilisateurDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String nom;
    private final String prenom;
    private final String email;
    private final String motDePasseHache;
    private final Role role;
    private final boolean actif;

    public UtilisateurDetails(Long id, String nom, String prenom, String email,
                              String motDePasseHache, Role role, boolean actif) {
        this.id = id;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.motDePasseHache = motDePasseHache;
        this.role = role;
        this.actif = actif;
    }

    public static UtilisateurDetails de(Utilisateur u) {
        return new UtilisateurDetails(u.getId(), u.getNom(), u.getPrenom(), u.getEmail(),
                u.getMotDePasseHache(), u.getRole(), u.isActif());
    }

    public Long getId() { return id; }
    public String getNom() { return nom; }
    public String getPrenom() { return prenom; }
    public Role getRole() { return role; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getPassword() { return motDePasseHache; }
    @Override public String getUsername() { return email; }
    /** Un compte inactif (en attente de validation ou désactivé) ne peut pas se connecter. */
    @Override public boolean isEnabled() { return actif; }
}
