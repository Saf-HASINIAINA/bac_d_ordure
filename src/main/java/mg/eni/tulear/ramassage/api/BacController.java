package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.BacDto;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.service.ConsultationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bacs")
public class BacController {

    private final ConsultationService consultation;

    public BacController(ConsultationService consultation) {
        this.consultation = consultation;
    }

    @GetMapping
    public List<BacDto> lister(@RequestParam(required = false) Long zoneId,
                               @RequestParam(required = false) EtatBac etat) {
        return consultation.bacs(zoneId, etat);
    }

    @GetMapping("/{id}")
    public BacDto obtenir(@PathVariable Long id) {
        return consultation.bac(id);
    }
}
