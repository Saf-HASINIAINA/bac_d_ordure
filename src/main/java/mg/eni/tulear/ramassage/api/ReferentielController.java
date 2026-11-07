package mg.eni.tulear.ramassage.api;

import mg.eni.tulear.ramassage.api.dto.CamionDto;
import mg.eni.tulear.ramassage.api.dto.ChauffeurDto;
import mg.eni.tulear.ramassage.api.dto.ConfigurationDto;
import mg.eni.tulear.ramassage.api.dto.StatistiquesDto;
import mg.eni.tulear.ramassage.api.dto.ZoneDto;
import mg.eni.tulear.ramassage.service.ConsultationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferentielController {

    private final ConsultationService consultation;
    private final double depotLatitude;
    private final double depotLongitude;

    public ReferentielController(ConsultationService consultation,
                                 @Value("${tournee.depot-latitude:-23.3550}") double depotLatitude,
                                 @Value("${tournee.depot-longitude:43.6700}") double depotLongitude) {
        this.consultation = consultation;
        this.depotLatitude = depotLatitude;
        this.depotLongitude = depotLongitude;
    }

    @GetMapping("/zones")
    public List<ZoneDto> zones() {
        return consultation.zones();
    }

    @GetMapping("/camions")
    public List<CamionDto> camions() {
        return consultation.camions();
    }

    @GetMapping("/chauffeurs")
    public List<ChauffeurDto> chauffeurs() {
        return consultation.chauffeurs();
    }

    @GetMapping("/configuration")
    public ConfigurationDto configuration() {
        return new ConfigurationDto(depotLatitude, depotLongitude);
    }

    @GetMapping("/statistiques")
    public StatistiquesDto statistiques() {
        return consultation.statistiques();
    }
}
