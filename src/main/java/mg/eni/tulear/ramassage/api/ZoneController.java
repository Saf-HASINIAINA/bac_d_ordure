package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.ZoneDto;
import mg.eni.tulear.ramassage.api.dto.ZoneRequest;
import mg.eni.tulear.ramassage.service.ConsultationService;
import mg.eni.tulear.ramassage.service.ParcService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Zones : lecture pour administrateur et superviseur ; création, modification, suppression pour l'administrateur. */
@RestController
@RequestMapping("/api/zones")
public class ZoneController {

    private final ConsultationService consultation;
    private final ParcService parc;

    public ZoneController(ConsultationService consultation, ParcService parc) {
        this.consultation = consultation;
        this.parc = parc;
    }

    @GetMapping
    public List<ZoneDto> lister() {
        return consultation.zones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ZoneDto creer(@Valid @RequestBody ZoneRequest requete) {
        return parc.creerZone(requete);
    }

    @PutMapping("/{id}")
    public ZoneDto modifier(@PathVariable Long id, @Valid @RequestBody ZoneRequest requete) {
        return parc.modifierZone(id, requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        parc.supprimerZone(id);
    }
}
