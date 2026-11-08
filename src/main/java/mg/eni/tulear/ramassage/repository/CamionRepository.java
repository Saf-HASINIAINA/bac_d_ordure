package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CamionRepository extends JpaRepository<Camion, Long> {
    List<Camion> findByStatut(StatutCamion statut);
    java.util.Optional<Camion> findByImmatriculationIgnoreCase(String immatriculation);
}
