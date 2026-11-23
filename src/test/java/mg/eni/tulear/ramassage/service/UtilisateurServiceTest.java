package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.CreerUtilisateurRequest;
import mg.eni.tulear.ramassage.api.dto.InscriptionRequest;
import mg.eni.tulear.ramassage.api.dto.UtilisateurDto;
import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.repository.UtilisateurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UtilisateurServiceTest {

    private final PasswordEncoder encodeur = new BCryptPasswordEncoder(4);
    private UtilisateurRepository repository;
    private UtilisateurService service;

    @BeforeEach
    void init() {
        repository = mock(UtilisateurRepository.class);
        when(repository.saveAndFlush(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new UtilisateurService(repository, encodeur, new GenerateurMotDePasse());
    }

    private InscriptionRequest inscription() {
        return new InscriptionRequest(" Rasoa ", "Paul", "  Paul.Rasoa@Tulear.MG ", "motdepasse1", "P-123");
    }

    @Test
    void inscriptionCreeUnChauffeurInactifAuMotDePasseHache() {
        UtilisateurDto dto = service.inscrire(inscription());

        ArgumentCaptor<Utilisateur> capture = ArgumentCaptor.forClass(Utilisateur.class);
        verify(repository).saveAndFlush(capture.capture());
        Utilisateur enregistre = capture.getValue();
        assertInstanceOf(Chauffeur.class, enregistre);
        assertFalse(enregistre.isActif(), "un compte inscrit publiquement doit attendre la validation");
        assertNotEquals("motdepasse1", enregistre.getMotDePasseHache());
        assertTrue(encodeur.matches("motdepasse1", enregistre.getMotDePasseHache()));
        assertEquals("paul.rasoa@tulear.mg", enregistre.getEmail(), "email normalisé (minuscules, sans espaces)");
        assertEquals("Rasoa", enregistre.getNom());
        assertEquals(Role.CHAUFFEUR, dto.role());
        assertFalse(dto.actif());
    }

    @Test
    void inscriptionRefuseUnEmailDejaUtilise() {
        when(repository.existsByEmailIgnoreCase("paul.rasoa@tulear.mg")).thenReturn(true);
        RegleMetierException e = assertThrows(RegleMetierException.class, () -> service.inscrire(inscription()));
        assertTrue(e.getMessage().contains("déjà utilisé"));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void inscriptionSimultanneeAvecLeMemeEmailEstAussiRefusee() {
        when(repository.saveAndFlush(any(Utilisateur.class))).thenThrow(new DataIntegrityViolationException("doublon"));
        assertThrows(RegleMetierException.class, () -> service.inscrire(inscription()));
    }

    @Test
    void adminPeutCreerChaqueRoleEtLeCompteEstActif() {
        for (Role role : Role.values()) {
            CreerUtilisateurRequest r = new CreerUtilisateurRequest(role, "N", "P", role + "@tulear.mg", "motdepasse1", "P-1");
            UtilisateurDto dto = service.creer(r);
            assertEquals(role, dto.role());
            assertTrue(dto.actif());
        }
    }

    @Test
    void creerUnChauffeurExigeUnNumeroDePermis() {
        CreerUtilisateurRequest r = new CreerUtilisateurRequest(Role.CHAUFFEUR, "N", "P", "c@tulear.mg", "motdepasse1", "  ");
        assertThrows(RequeteInvalideException.class, () -> service.creer(r));
    }

    @Test
    void activerEtDesactiverUnCompte() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", "h", "P1");
        c.setActif(false);
        when(repository.findById(9L)).thenReturn(Optional.of(c));

        assertTrue(service.activer(9L).actif());
        assertFalse(service.desactiver(9L, 1L).actif());
    }

    @Test
    void unAdministrateurNePeutPasDesactiverSonPropreCompte() {
        assertThrows(RegleMetierException.class, () -> service.desactiver(1L, 1L));
        verifyNoInteractions(repository);
    }

    @Test
    void utilisateurInconnuRenvoieIntrouvable() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.activer(99L));
    }

    @Test
    void changerSonMotDePasseExigeLAncienEtLeHacheLeNouveau() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", encodeur.encode("ancien-mdp-1"), "P1");
        when(repository.findById(3L)).thenReturn(Optional.of(c));

        service.changerMotDePasse(3L, "ancien-mdp-1", "nouveau-mdp-2");

        assertTrue(encodeur.matches("nouveau-mdp-2", c.getMotDePasseHache()));
        assertFalse(encodeur.matches("ancien-mdp-1", c.getMotDePasseHache()));
        assertNotEquals("nouveau-mdp-2", c.getMotDePasseHache());
    }

    @Test
    void unMauvaisAncienMotDePasseEstRefuseEtRienNeChange() {
        String hache = encodeur.encode("ancien-mdp-1");
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", hache, "P1");
        when(repository.findById(3L)).thenReturn(Optional.of(c));

        assertThrows(RequeteInvalideException.class, () -> service.changerMotDePasse(3L, "pas-le-bon", "nouveau-mdp-2"));
        assertEquals(hache, c.getMotDePasseHache());
    }

    @Test
    void leNouveauMotDePasseDoitDifferer() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", encodeur.encode("ancien-mdp-1"), "P1");
        when(repository.findById(3L)).thenReturn(Optional.of(c));
        assertThrows(RequeteInvalideException.class, () -> service.changerMotDePasse(3L, "ancien-mdp-1", "ancien-mdp-1"));
    }

    @Test
    void reinitialiserGenereUnMotDePasseTemporaireQuiFonctionne() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", encodeur.encode("oublie-mdp-1"), "P1");
        when(repository.findById(4L)).thenReturn(Optional.of(c));

        String temporaire = service.reinitialiserMotDePasse(4L, 1L);

        assertEquals(12, temporaire.length());
        assertTrue(encodeur.matches(temporaire, c.getMotDePasseHache()), "le nouveau hash correspond au mot de passe rendu");
        assertFalse(encodeur.matches("oublie-mdp-1", c.getMotDePasseHache()), "l'ancien mot de passe ne marche plus");
    }

    @Test
    void reinitialiserDonneDesMotsDePasseDifferents() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", "h", "P1");
        when(repository.findById(4L)).thenReturn(Optional.of(c));
        assertNotEquals(service.reinitialiserMotDePasse(4L, 1L), service.reinitialiserMotDePasse(4L, 1L));
    }

    @Test
    void unAdministrateurNeReinitialisePasSonPropreMotDePasse() {
        assertThrows(RegleMetierException.class, () -> service.reinitialiserMotDePasse(1L, 1L));
        verifyNoInteractions(repository);
    }

    @Test
    void reinitialiserUnCompteInconnuRenvoieIntrouvable() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.reinitialiserMotDePasse(99L, 1L));
    }

    @Test
    void reinitialisationDeSecoursParEmailIgnoreLaCasse() {
        Administrateur a = new Administrateur("N", "P", "admin@tulear.mg", encodeur.encode("oublie-mdp-1"));
        when(repository.findByEmailIgnoreCase("admin@tulear.mg")).thenReturn(Optional.of(a));

        service.reinitialiserParEmail("  Admin@Tulear.MG ", "nouveau-mdp-2");

        assertTrue(encodeur.matches("nouveau-mdp-2", a.getMotDePasseHache()));
    }

    @Test
    void reinitialisationDeSecoursAvecEmailInconnuEchoue() {
        when(repository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        assertThrows(RessourceIntrouvableException.class, () -> service.reinitialiserParEmail("x@tulear.mg", "nouveau-mdp-2"));
    }

    @Test
    void unMotDePasseReinitialiseDoitEtreChangeALaConnexionSuivante() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", encodeur.encode("oublie-mdp-1"), "P1");
        when(repository.findById(4L)).thenReturn(Optional.of(c));
        assertFalse(c.isMdpAChanger());

        service.reinitialiserMotDePasse(4L, 1L);

        assertTrue(c.isMdpAChanger());
    }

    @Test
    void laReinitialisationDeSecoursImposeAussiUnChangement() {
        Administrateur a = new Administrateur("N", "P", "admin@tulear.mg", "h");
        when(repository.findByEmailIgnoreCase("admin@tulear.mg")).thenReturn(Optional.of(a));
        service.reinitialiserParEmail("admin@tulear.mg", "nouveau-mdp-2");
        assertTrue(a.isMdpAChanger());
    }

    @Test
    void choisirSonMotDePasseLeveLObligation() {
        Chauffeur c = new Chauffeur("N", "P", "c@tulear.mg", encodeur.encode("temporaire-1"), "P1");
        c.imposerMotDePasse(c.getMotDePasseHache());
        when(repository.findById(3L)).thenReturn(Optional.of(c));

        service.changerMotDePasse(3L, "temporaire-1", "choisi-par-moi-2");

        assertFalse(c.isMdpAChanger());
    }

    @Test
    void uneInscriptionNEstPasForceeAChangerSonMotDePasse() {
        when(repository.existsByEmailIgnoreCase(any())).thenReturn(false);
        when(repository.saveAndFlush(any())).thenAnswer(i -> i.getArgument(0));
        service.inscrire(inscription());
        ArgumentCaptor<Utilisateur> capteur = ArgumentCaptor.forClass(Utilisateur.class);
        verify(repository).saveAndFlush(capteur.capture());
        assertFalse(capteur.getValue().isMdpAChanger());
    }
}
