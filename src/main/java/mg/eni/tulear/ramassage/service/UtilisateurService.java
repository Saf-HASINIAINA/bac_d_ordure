package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.CreerUtilisateurRequest;
import mg.eni.tulear.ramassage.api.dto.InscriptionRequest;
import mg.eni.tulear.ramassage.api.dto.UtilisateurDto;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.repository.UtilisateurRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Inscription, création et activation des comptes. */
@Service
public class UtilisateurService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final GenerateurMotDePasse generateur;

    public UtilisateurService(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder,
                              GenerateurMotDePasse generateur) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.generateur = generateur;
    }

    /** Inscription publique : toujours un chauffeur, inactif tant qu'un administrateur ne l'a pas validé. */
    @Transactional
    public UtilisateurDto inscrire(InscriptionRequest r) {
        Chauffeur chauffeur = new Chauffeur(r.nom().trim(), r.prenom().trim(), normaliser(r.email()),
                passwordEncoder.encode(r.motDePasse()), r.numeroPermis().trim());
        chauffeur.setActif(false);
        return enregistrer(chauffeur);
    }

    /** Création par l'administrateur : compte actif, rôle au choix. */
    @Transactional
    public UtilisateurDto creer(CreerUtilisateurRequest r) {
        String nom = r.nom().trim();
        String prenom = r.prenom().trim();
        String email = normaliser(r.email());
        String hash = passwordEncoder.encode(r.motDePasse());
        Utilisateur utilisateur = switch (r.role()) {
            case ADMIN -> new Administrateur(nom, prenom, email, hash);
            case SUPERVISEUR -> new Superviseur(nom, prenom, email, hash);
            case CHAUFFEUR -> {
                if (r.numeroPermis() == null || r.numeroPermis().isBlank()) {
                    throw new RequeteInvalideException("Le numéro de permis est obligatoire pour un chauffeur");
                }
                yield new Chauffeur(nom, prenom, email, hash, r.numeroPermis().trim());
            }
        };
        return enregistrer(utilisateur);
    }

    @Transactional(readOnly = true)
    public List<UtilisateurDto> lister() {
        return utilisateurRepository.findAll().stream().map(UtilisateurDto::from).toList();
    }

    @Transactional
    public UtilisateurDto activer(Long id) {
        Utilisateur u = charger(id);
        u.setActif(true);
        return UtilisateurDto.from(u);
    }

    /** @param moiId l'administrateur qui agit : il ne peut pas se désactiver lui-même. */
    @Transactional
    public UtilisateurDto desactiver(Long id, Long moiId) {
        if (id.equals(moiId)) {
            throw new RegleMetierException("Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur u = charger(id);
        u.setActif(false);
        return UtilisateurDto.from(u);
    }

    /** Changement par l'utilisateur lui-même : l'ancien mot de passe est exigé. */
    @Transactional
    public void changerMotDePasse(Long id, String ancien, String nouveau) {
        Utilisateur u = charger(id);
        if (!passwordEncoder.matches(ancien, u.getMotDePasseHache())) {
            throw new RequeteInvalideException("Le mot de passe actuel est incorrect");
        }
        if (ancien.equals(nouveau)) {
            throw new RequeteInvalideException("Le nouveau mot de passe doit être différent de l'ancien");
        }
        u.changerMotDePasse(passwordEncoder.encode(nouveau));
    }

    /**
     * Mot de passe oublié : l'administrateur génère un mot de passe temporaire pour un autre compte.
     * @param moiId l'administrateur qui agit : pour son propre compte, il utilise le changement de mot de passe.
     * @return le mot de passe en clair, à communiquer une seule fois (seul le hash est conservé)
     */
    @Transactional
    public String reinitialiserMotDePasse(Long id, Long moiId) {
        if (id.equals(moiId)) {
            throw new RegleMetierException("Pour votre propre compte, utilisez « Mot de passe » (changement avec l'ancien)");
        }
        Utilisateur u = charger(id);
        String temporaire = generateur.generer();
        u.changerMotDePasse(passwordEncoder.encode(temporaire));
        return temporaire;
    }

    /** Commande de secours (serveur) : réinitialise n'importe quel compte, y compris le dernier administrateur. */
    @Transactional
    public void reinitialiserParEmail(String email, String nouveauMotDePasse) {
        Utilisateur u = utilisateurRepository.findByEmailIgnoreCase(normaliser(email))
                .orElseThrow(() -> new RessourceIntrouvableException("Aucun compte avec l'e-mail : " + email));
        u.changerMotDePasse(passwordEncoder.encode(nouveauMotDePasse));
    }

    private UtilisateurDto enregistrer(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmailIgnoreCase(utilisateur.getEmail())) {
            throw new RegleMetierException("Cet email est déjà utilisé");
        }
        try {
            return UtilisateurDto.from(utilisateurRepository.saveAndFlush(utilisateur));
        } catch (DataIntegrityViolationException e) { // inscription simultanée avec le même email
            throw new RegleMetierException("Cet email est déjà utilisé");
        }
    }

    private Utilisateur charger(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Utilisateur introuvable : " + id));
    }

    private static String normaliser(String email) {
        return email.trim().toLowerCase();
    }
}
