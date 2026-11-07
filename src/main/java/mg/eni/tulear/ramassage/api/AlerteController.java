package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.AlerteDto;
import mg.eni.tulear.ramassage.service.AlerteService;
import mg.eni.tulear.ramassage.service.ConsultationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alertes")
public class AlerteController {

    private final ConsultationService consultation;
    private final AlerteService alerteService;

    public AlerteController(ConsultationService consultation, AlerteService alerteService) {
        this.consultation = consultation;
        this.alerteService = alerteService;
    }

    /** ?traitee=false pour ne voir que les alertes à traiter. */
    @GetMapping
    public List<AlerteDto> lister(@RequestParam(required = false) Boolean traitee) {
        return consultation.alertes(traitee);
    }

    @PostMapping("/{id}/traiter")
    public AlerteDto traiter(@PathVariable Long id) {
        return alerteService.traiter(id);
    }
}
