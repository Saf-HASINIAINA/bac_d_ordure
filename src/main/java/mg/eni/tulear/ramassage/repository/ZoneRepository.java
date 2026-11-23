package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ZoneRepository extends JpaRepository<Zone, Long> {
    java.util.Optional<Zone> findByNomIgnoreCase(String nom);
}
