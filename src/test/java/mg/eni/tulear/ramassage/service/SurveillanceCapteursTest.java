package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.TypeCapteur;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.repository.CapteurRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class SurveillanceCapteursTest {

    private static final Instant MAINTENANT = Instant.parse("2026-11-07T12:00:00Z");
    private final Seuils seuils = Seuils.parDefaut(); // délai de panne : 30 min

    private CapteurRepository repository;
    private ServiceAlerte serviceAlerte;
    private SurveillanceCapteurs surveillance;
    private Bac bac;

    @BeforeEach
    void init() {
        repository = mock(CapteurRepository.class);
        serviceAlerte = mock(ServiceAlerte.class);
        surveillance = new SurveillanceCapteurs(repository, mock(ParametreService.class), serviceAlerte);
        bac = new Bac("BAC-001", new Zone("Z", 24, 0, 0), 0, 0, 240, TypeDechet.MENAGER);
    }

    private Capteur capteurMuetDepuis(Duration silence) {
        Capteur c = new CapteurGaz(bac);
        c.setDerniereMesure(MAINTENANT.minus(silence));
        return c;
    }

    @Test
    void unCapteurMuetDepuisPlusDuDelaiDeclencheUneAlerte() {
        when(repository.findActifsAvecBac()).thenReturn(List.of(capteurMuetDepuis(Duration.ofMinutes(45))));

        assertEquals(1, surveillance.verifier(MAINTENANT, seuils));
        verify(serviceAlerte).signalerCapteurEnPanne(bac, TypeCapteur.GAZ, 45);
    }

    @Test
    void unCapteurRecemmentVuNeDeclencheRien() {
        when(repository.findActifsAvecBac()).thenReturn(List.of(capteurMuetDepuis(Duration.ofMinutes(5))));

        assertEquals(0, surveillance.verifier(MAINTENANT, seuils));
        verifyNoInteractions(serviceAlerte);
    }

    @Test
    void exactementAuDelaiLeCapteurEstEncoreConsideCommeValide() {
        when(repository.findActifsAvecBac()).thenReturn(List.of(capteurMuetDepuis(Duration.ofMinutes(30))));

        assertEquals(0, surveillance.verifier(MAINTENANT, seuils));
    }

    @Test
    void unCapteurJamaisVuEstIgnore() {
        Capteur neuf = new CapteurNiveau(bac); // derniereMesure = null
        when(repository.findActifsAvecBac()).thenReturn(List.of(neuf));

        assertEquals(0, surveillance.verifier(MAINTENANT, seuils));
        verifyNoInteractions(serviceAlerte);
    }

    @Test
    void plusieursCapteursSontTousExamines() {
        when(repository.findActifsAvecBac()).thenReturn(List.of(
                capteurMuetDepuis(Duration.ofMinutes(90)), capteurMuetDepuis(Duration.ofMinutes(1)),
                capteurMuetDepuis(Duration.ofHours(5))));

        assertEquals(2, surveillance.verifier(MAINTENANT, seuils));
        verify(serviceAlerte, times(2)).signalerCapteurEnPanne(eq(bac), any(), anyLong());
    }

    @Test
    void ledelaiVientDesSeuilsConfigures() {
        Seuils court = new Seuils(40, 70, 90, 80, 2, 300); // 2 minutes
        when(repository.findActifsAvecBac()).thenReturn(List.of(capteurMuetDepuis(Duration.ofMinutes(3))));

        assertEquals(1, surveillance.verifier(MAINTENANT, court));
    }
}
