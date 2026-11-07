package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.AlerteDto;
import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final ServiceAlerte serviceAlerte;

    public AlerteService(AlerteRepository alerteRepository, ServiceAlerte serviceAlerte) {
        this.alerteRepository = alerteRepository;
        this.serviceAlerte = serviceAlerte;
    }

    /** Marque l'alerte comme traitée et autorise la création d'une nouvelle alerte du même type. */
    @Transactional
    public AlerteDto traiter(Long id) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Alerte introuvable : " + id));
        alerte.marquerTraitee();
        if (alerte.getBac() != null) {
            serviceAlerte.alerteTraitee(alerte.getBac().getId(), alerte.getType());
        }
        return AlerteDto.from(alerte);
    }
}
