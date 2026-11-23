package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.PageAuditDto;
import mg.eni.tulear.ramassage.service.JournalAuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Journal d'audit : réservé aux administrateurs (voir SecurityConfig). */
@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final JournalAuditService journal;

    public AuditController(JournalAuditService journal) {
        this.journal = journal;
    }

    @GetMapping
    public PageAuditDto lister(@RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "50") int taille) {
        return journal.lister(page, taille);
    }
}
