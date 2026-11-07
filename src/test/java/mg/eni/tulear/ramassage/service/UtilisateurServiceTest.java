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
        service = new UtilisateurService(repository, encodeur);
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
}
