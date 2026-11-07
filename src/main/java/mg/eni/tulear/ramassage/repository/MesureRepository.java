package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MesureRepository extends JpaRepository<Mesure, Long> {
}
