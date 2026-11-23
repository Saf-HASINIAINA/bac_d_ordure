package mg.eni.tulear.ramassage.api.dto;

import java.util.List;

/** Une page du journal, de la plus récente à la plus ancienne. */
public record PageAuditDto(List<EntreeAuditDto> entrees, long total, int page, int taille, boolean derniere) { }
