package mg.eni.tulear.ramassage.securite;

import mg.eni.tulear.ramassage.api.RapportController;
import mg.eni.tulear.ramassage.api.dto.RapportDto;
import mg.eni.tulear.ramassage.domain.enums.Role;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.service.RapportService;
import mg.eni.tulear.ramassage.service.RequeteInvalideException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RapportController.class)
@Import(SecurityConfig.class)
class RapportApiTest {

    @Autowired MockMvc mvc;
    @MockBean RapportService rapportService;
    @MockBean UserDetailsService userDetailsService;

    private static RequestPostProcessor connecte(Role role) {
        return user(new UtilisateurDetails(1L, "N", "P", role.name().toLowerCase() + "@tulear.mg", "h", role, true));
    }

    private RapportDto rapport() {
        return new RapportDto(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7), 216.0, 2, 6, 5.5, 88.0,
                new EnumMap<>(TypeAlerte.class), List.of(), List.of());
    }

    @Test
    void unSuperviseurEtUnAdminPeuventConsulterLeRapport() throws Exception {
        when(rapportService.generer(any(), any())).thenReturn(rapport());
        for (Role role : List.of(Role.SUPERVISEUR, Role.ADMIN)) {
            mvc.perform(get("/api/rapports").with(connecte(role)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tonnageTotalKg").value(216.0))
                    .andExpect(jsonPath("$.nombreCollectes").value(6));
        }
    }

    @Test
    void unChauffeurNaPasAccesAuxRapports() throws Exception {
        mvc.perform(get("/api/rapports").with(connecte(Role.CHAUFFEUR))).andExpect(status().isForbidden());
        mvc.perform(get("/api/rapports/export.csv").with(connecte(Role.CHAUFFEUR))).andExpect(status().isForbidden());
        verifyNoInteractions(rapportService);
    }

    @Test
    void sansConnexionLeRapportRenvoie401() throws Exception {
        mvc.perform(get("/api/rapports")).andExpect(status().isUnauthorized());
    }

    @Test
    void lesDatesSontTransmiseesAuService() throws Exception {
        when(rapportService.generer(any(), any())).thenReturn(rapport());
        mvc.perform(get("/api/rapports").param("debut", "2026-11-01").param("fin", "2026-11-07")
                .with(connecte(Role.SUPERVISEUR))).andExpect(status().isOk());
        verify(rapportService).generer(eq(LocalDate.of(2026, 11, 1)), eq(LocalDate.of(2026, 11, 7)));
    }

    @Test
    void uneDateInvalideRenvoie400() throws Exception {
        mvc.perform(get("/api/rapports").param("debut", "pas-une-date").with(connecte(Role.SUPERVISEUR)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unePeriodeIncoherenteRenvoie400() throws Exception {
        when(rapportService.generer(any(), any()))
                .thenThrow(new RequeteInvalideException("La date de début doit précéder la date de fin"));
        mvc.perform(get("/api/rapports").param("debut", "2026-11-09").param("fin", "2026-11-01")
                        .with(connecte(Role.SUPERVISEUR)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("précéder")));
    }

    @Test
    void lExportCsvEstUnTelechargementUtf8() throws Exception {
        when(rapportService.exporterCsv(any(), any())).thenReturn("﻿Date;Heure\r\n");
        when(rapportService.nomFichier(any(), any())).thenReturn("rapport-collectes-2026-11-01_2026-11-07.csv");

        mvc.perform(get("/api/rapports/export.csv").with(connecte(Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Type", containsString("UTF-8")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("rapport-collectes-2026-11-01_2026-11-07.csv")))
                .andExpect(content().string("﻿Date;Heure\r\n"));
    }
}
