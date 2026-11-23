package mg.eni.tulear.ramassage.api;

import jakarta.validation.Valid;
import mg.eni.tulear.ramassage.api.dto.CamionDto;
import mg.eni.tulear.ramassage.api.dto.CamionRequest;
import mg.eni.tulear.ramassage.service.ConsultationService;
import mg.eni.tulear.ramassage.service.ParcService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Camions : lecture pour tous (la carte les affiche) ; création, modification, suppression pour l'administrateur ;
 * panne et remise à disposition pour l'administrateur et le superviseur.
 */
@RestController
@RequestMapping("/api/camions")
public class CamionController {

    private final ConsultationService consultation;
    private final ParcService parc;

    public CamionController(ConsultationService consultation, ParcService parc) {
        this.consultation = consultation;
        this.parc = parc;
    }

    @GetMapping
    public List<CamionDto> lister() {
        return consultation.camions();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CamionDto creer(@Valid @RequestBody CamionRequest requete) {
        return parc.creerCamion(requete);
    }

    @PutMapping("/{id}")
    public CamionDto modifier(@PathVariable Long id, @Valid @RequestBody CamionRequest requete) {
        return parc.modifierCamion(id, requete);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void supprimer(@PathVariable Long id) {
        parc.supprimerCamion(id);
    }

    @PostMapping("/{id}/panne")
    public CamionDto declarerEnPanne(@PathVariable Long id) {
        return parc.declarerCamionEnPanne(id);
    }

    @PostMapping("/{id}/disponible")
    public CamionDto remettreDisponible(@PathVariable Long id) {
        return parc.remettreCamionDisponible(id);
    }
}
