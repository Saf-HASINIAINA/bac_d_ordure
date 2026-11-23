package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.EntreeAuditDto;
import mg.eni.tulear.ramassage.api.dto.PageAuditDto;
import mg.eni.tulear.ramassage.domain.EntreeAudit;
import mg.eni.tulear.ramassage.repository.EntreeAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Journal d'audit : enregistre qui a fait quoi, et le restitue à l'administrateur. */
@Service
public class JournalAuditService {

    public static final int TAILLE_MAX = 200;
    private static final Logger log = LoggerFactory.getLogger(JournalAuditService.class);

    private final EntreeAuditRepository repository;

    public JournalAuditService(EntreeAuditRepository repository) {
        this.repository = repository;
    }

    /**
     * Enregistre une action. Ne lève jamais d'exception : un incident sur le journal ne doit pas
     * faire échouer l'action de l'utilisateur (l'erreur est écrite dans les logs).
     */
    public void enregistrer(Long utilisateurId, String email, String action, String methode, String chemin,
                            int statut, String adresseIp) {
        try {
            repository.save(new EntreeAudit(utilisateurId, email == null || email.isBlank() ? "anonyme" : email,
                    action, methode, chemin, statut, adresseIp));
        } catch (RuntimeException e) {
            log.error("Journal d'audit : enregistrement impossible ({} {}) : {}", methode, chemin, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public PageAuditDto lister(int page, int taille) {
        int t = Math.min(Math.max(taille, 1), TAILLE_MAX);
        Page<EntreeAudit> resultat = repository.findAll(
                PageRequest.of(Math.max(page, 0), t, Sort.by(Sort.Direction.DESC, "dateAction", "id")));
        return new PageAuditDto(resultat.getContent().stream().map(EntreeAuditDto::from).toList(),
                resultat.getTotalElements(), resultat.getNumber(), resultat.getSize(), resultat.isLast());
    }
}
