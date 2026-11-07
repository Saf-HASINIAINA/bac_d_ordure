package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.domain.enums.TypeMesure;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ServiceAlerteTest {

    private AlerteRepository repository;
    private Observateur observateur;
    private ServiceAlerte service;
    private Bac bac;

    @BeforeEach
    void init() {
        repository = mock(AlerteRepository.class);
        observateur = mock(Observateur.class);
        when(repository.save(any(Alerte.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new ServiceAlerte(repository, List.of(observateur));
        bac = new Bac("BAC-T", new Zone("Test", 24, 0, 0), 0, 0, 240, TypeDechet.MENAGER);
        ReflectionTestUtils.setField(bac, "id", 1L);
    }

    private Mesure mesure(TypeMesure type, double valeur) {
        return new Mesure(null, type, valeur);
    }

    @Test
    void niveauAuDessusDuSeuilCreeUneAlerteEtNotifieLesObservateurs() {
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 85), Seuils.parDefaut());
        verify(repository).save(any(Alerte.class));
        verify(observateur).notifier(any(Alerte.class));
    }

    @Test
    void niveauSousLeSeuilNeCreeRien() {
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 60), Seuils.parDefaut());
        verifyNoInteractions(observateur);
        verify(repository, never()).save(any());
    }

    @Test
    void pasDeDoublonTantQueLAlerteN_estPasTraitee() {
        when(repository.existsByBacIdAndTypeAndTraiteeFalse(1L, TypeAlerte.BAC_PLEIN)).thenReturn(true);
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 95), Seuils.parDefaut());
        verify(repository, never()).save(any());
        verifyNoInteractions(observateur);
    }

    @Test
    void picDeGazCreeUneAlerteIncendie() {
        service.evaluerMesure(bac, mesure(TypeMesure.GAZ, 400), Seuils.parDefaut());
        verify(repository).save(argThat(a -> a.getType() == TypeAlerte.RISQUE_INCENDIE));
    }

    @Test
    void alerteDejaConnueNeRequeteplusLaBase() {
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 85), Seuils.parDefaut());
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 90), Seuils.parDefaut());
        verify(repository, times(1)).existsByBacIdAndTypeAndTraiteeFalse(1L, TypeAlerte.BAC_PLEIN);
        verify(repository, times(1)).save(any(Alerte.class));
    }

    @Test
    void apresTraitementUneNouvelleAlertePeutEtreCreee() {
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 85), Seuils.parDefaut());
        service.alerteTraitee(1L, TypeAlerte.BAC_PLEIN);
        service.evaluerMesure(bac, mesure(TypeMesure.NIVEAU, 85), Seuils.parDefaut());
        verify(repository, times(2)).save(any(Alerte.class));
    }
}
