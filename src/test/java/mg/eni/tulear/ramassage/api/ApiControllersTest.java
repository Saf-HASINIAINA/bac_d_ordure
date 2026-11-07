package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.*;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeDechet;
import mg.eni.tulear.ramassage.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {BacController.class, TourneeController.class, AlerteController.class,
        ReferentielController.class})
class ApiControllersTest {

    @Autowired MockMvc mvc;
    @MockBean ConsultationService consultation;
    @MockBean TourneeFacade tournees;
    @MockBean AlerteService alerteService;

    private BacDto bac() {
        return new BacDto(1L, "BAC-001", 1L, "Centre-ville", -23.35, 43.67, 240, 85, 30.6,
                TypeDechet.MENAGER, EtatBac.PLEIN, null);
    }

    private TourneeDto tournee(StatutTournee statut) {
        return new TourneeDto(5L, LocalDate.of(2026, 11, 7), statut, 2.3, 1L, "Centre-ville", 2L, "1234 TAA",
                3L, "Jean Rakoto", 0, 30.6, List.of());
    }

    @Test
    void listerLesBacsRenvoieDuJson() throws Exception {
        when(consultation.bacs(null, null)).thenReturn(List.of(bac()));
        mvc.perform(get("/api/bacs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("BAC-001"))
                .andExpect(jsonPath("$[0].etat").value("PLEIN"))
                .andExpect(jsonPath("$[0].niveauPourcent").value(85.0));
    }

    @Test
    void filtrerLesBacsParZoneEtEtat() throws Exception {
        when(consultation.bacs(1L, EtatBac.PLEIN)).thenReturn(List.of(bac()));
        mvc.perform(get("/api/bacs").param("zoneId", "1").param("etat", "PLEIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void bacInconnuRenvoie404() throws Exception {
        when(consultation.bac(99L)).thenThrow(new RessourceIntrouvableException("Bac introuvable : 99"));
        mvc.perform(get("/api/bacs/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statut").value(404))
                .andExpect(jsonPath("$.message").value("Bac introuvable : 99"));
    }

    @Test
    void etatInvalideRenvoie400() throws Exception {
        mvc.perform(get("/api/bacs").param("etat", "BIDON"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void planifierUneTourneeRenvoie201() throws Exception {
        when(tournees.planifier(any())).thenReturn(tournee(StatutTournee.PLANIFIEE));
        mvc.perform(post("/api/tournees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":1,\"camionId\":2,\"chauffeurId\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.statut").value("PLANIFIEE"));
    }

    @Test
    void planifierSansZoneRenvoie400() throws Exception {
        mvc.perform(post("/api/tournees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"camionId\":2,\"chauffeurId\":3}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("zoneId")));
    }

    @Test
    void jsonIllisibleRenvoie400() throws Exception {
        mvc.perform(post("/api/tournees").contentType(MediaType.APPLICATION_JSON).content("{pas du json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void regleDeGestionViolEeRenvoie409() throws Exception {
        when(tournees.planifier(any())).thenThrow(new RegleMetierException("Le chauffeur a déjà une tournée active"));
        mvc.perform(post("/api/tournees").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zoneId\":1,\"camionId\":2,\"chauffeurId\":3}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Le chauffeur a déjà une tournée active"));
    }

    @Test
    void demarrerEtConfirmerUneCollecte() throws Exception {
        when(tournees.demarrer(5L)).thenReturn(tournee(StatutTournee.EN_COURS));
        when(tournees.confirmerCollecte(5L, 10L)).thenReturn(tournee(StatutTournee.EN_COURS));
        mvc.perform(post("/api/tournees/5/demarrer")).andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EN_COURS"));
        mvc.perform(post("/api/tournees/5/collectes/10/confirmer")).andExpect(status().isOk());
    }

    @Test
    void statutDeTourneeInvalideRenvoie400() throws Exception {
        mvc.perform(get("/api/tournees").param("statut", "BIDON")).andExpect(status().isBadRequest());
    }

    @Test
    void statistiquesEtAlertes() throws Exception {
        when(consultation.statistiques()).thenReturn(new StatistiquesDto(24, Map.of(EtatBac.PLEIN, 3L), 41.5, 3, 1, 216.0));
        mvc.perform(get("/api/statistiques"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBacs").value(24))
                .andExpect(jsonPath("$.alertesOuvertes").value(3));

        when(alerteService.traiter(7L)).thenThrow(new RessourceIntrouvableException("Alerte introuvable : 7"));
        mvc.perform(post("/api/alertes/7/traiter")).andExpect(status().isNotFound());
    }
}
