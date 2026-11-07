package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    boolean existsByBacIdAndTypeAndTraiteeFalse(Long bacId, TypeAlerte type);
    List<Alerte> findByBacIdAndTypeAndTraiteeFalse(Long bacId, TypeAlerte type);
    List<Alerte> findByTraiteeFalseOrderByDateCreationDesc();
}
