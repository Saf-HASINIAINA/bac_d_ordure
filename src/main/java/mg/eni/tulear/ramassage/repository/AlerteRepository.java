package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    boolean existsByBacIdAndTypeAndTraiteeFalse(Long bacId, TypeAlerte type);
    List<Alerte> findByTraiteeFalseOrderByDateCreationDesc();
}
