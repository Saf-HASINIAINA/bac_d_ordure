package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.CamionDto;
import mg.eni.tulear.ramassage.api.dto.StatistiquesDto;
import mg.eni.tulear.ramassage.api.dto.ZoneDto;
import mg.eni.tulear.ramassage.service.ConsultationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferentielController {

    private final ConsultationService consultation;

    public ReferentielController(ConsultationService consultation) {
        this.consultation = consultation;
    }

    @GetMapping("/zones")
    public List<ZoneDto> zones() {
        return consultation.zones();
    }

    @GetMapping("/camions")
    public List<CamionDto> camions() {
        return consultation.camions();
    }

    @GetMapping("/statistiques")
    public StatistiquesDto statistiques() {
        return consultation.statistiques();
    }
}
