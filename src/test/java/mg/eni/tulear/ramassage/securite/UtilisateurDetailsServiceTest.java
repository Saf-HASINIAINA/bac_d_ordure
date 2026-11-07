package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.domain.Chauffeur;
import mg.eni.tulear.ramassage.domain.Superviseur;
import mg.eni.tulear.ramassage.repository.UtilisateurRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilisateurDetailsServiceTest {

    private final UtilisateurRepository repository = mock(UtilisateurRepository.class);
    private final UtilisateurDetailsService service = new UtilisateurDetailsService(repository);

    @Test
    void chargeLeRoleSousFormeDAutoriteSpring() {
        when(repository.findByEmailIgnoreCase("s@tulear.mg"))
                .thenReturn(Optional.of(new Superviseur("Rakoto", "Jean", "s@tulear.mg", "hash")));
        UserDetails d = service.loadUserByUsername(" s@tulear.mg ");
        assertEquals("s@tulear.mg", d.getUsername());
        assertEquals("hash", d.getPassword());
        assertTrue(d.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SUPERVISEUR")));
        assertTrue(d.isEnabled());
    }

    @Test
    void unCompteInactifEstDesactive() {
        Chauffeur c = new Chauffeur("Rasoa", "Paul", "c@tulear.mg", "hash", "P1");
        c.setActif(false);
        when(repository.findByEmailIgnoreCase("c@tulear.mg")).thenReturn(Optional.of(c));
        assertFalse(service.loadUserByUsername("c@tulear.mg").isEnabled());
    }

    @Test
    void emailInconnuLeveUneException() {
        when(repository.findByEmailIgnoreCase("x@tulear.mg")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("x@tulear.mg"));
    }
}
