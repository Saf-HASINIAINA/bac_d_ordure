package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Alerte;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {
    boolean existsByBacIdAndTypeAndTraiteeFalse(Long bacId, TypeAlerte type);
    List<Alerte> findByBacIdAndTypeAndTraiteeFalse(Long bacId, TypeAlerte type);
    List<Alerte> findByTraiteeFalseOrderByDateCreationDesc();
    List<Alerte> findAllByOrderByDateCreationDesc();
    long countByTraiteeFalse();

    @Query("select a.type, count(a) from Alerte a where a.dateCreation >= :debut and a.dateCreation < :fin group by a.type")
    List<Object[]> compterParType(@Param("debut") Instant debut, @Param("fin") Instant fin);
}
