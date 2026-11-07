package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BacRepository extends JpaRepository<Bac, Long> {
    List<Bac> findByZoneId(Long zoneId);
    List<Bac> findByNiveauPourcentGreaterThanEqual(double niveau);
}
