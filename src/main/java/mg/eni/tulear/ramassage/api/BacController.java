package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.BacDto;
import mg.eni.tulear.ramassage.api.dto.BacRequest;
import mg.eni.tulear.ramassage.domain.enums.EtatBac;
import mg.eni.tulear.ramassage.service.ConsultationService;
import mg.eni.tulear.ramassage.service.ParcService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Bacs : lecture pour tous ; installation, modification, suppression pour l'administrateur ;
 * hors service et remise en service pour l'administrateur et le superviseur.
 */
@RestController
@RequestMapping("/api/bacs")
public class BacController {

    private final ConsultationService consultation;
    private final ParcService parc;

    public BacController(ConsultationService consultation, ParcService parc) {
        this.consultation = consultation;
        this.parc = parc;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BacDto installer(@Valid @RequestBody BacRequest requete) {
        return parc.installerBac(requete);
    }

    @PutMapping("/{id}")
    public BacDto modifier(@PathVariable Long id, @Valid @RequestBody BacRequest requete) {
        return parc.modifierBac(id, requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        parc.supprimerBac(id);
    }

    @PostMapping("/{id}/hors-service")
    public BacDto mettreHorsService(@PathVariable Long id) {
        return parc.mettreBacHorsService(id);
    }

    @PostMapping("/{id}/remise-en-service")
    public BacDto remettreEnService(@PathVariable Long id) {
        return parc.remettreBacEnService(id);
    }
}
