package mg.eni.tulear.ramassage.api.dto;

import mg.eni.tulear.ramassage.domain.EntreeAudit;

import java.time.Instant;

public record EntreeAuditDto(Long id, Instant date, Long utilisateurId, String email, String action,
                             String methode, String chemin, int statut, String adresseIp) {
    public static EntreeAuditDto from(EntreeAudit e) {
        return new EntreeAuditDto(e.getId(), e.getDateAction(), e.getUtilisateurId(), e.getEmail(), e.getAction(),
                e.getMethode(), e.getChemin(), e.getStatut(), e.getAdresseIp());
    }
}
