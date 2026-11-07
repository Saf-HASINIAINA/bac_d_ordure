package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.*;
import mg.eni.tulear.ramassage.domain.enums.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BacRepository extends JpaRepository<Bac, Long> {
    List<Bac> findByZoneId(Long zoneId);
    List<Bac> findByNiveauPourcentGreaterThanEqual(double niveau);

    @Query("select b from Bac b join fetch b.zone order by b.code")
    List<Bac> findAllAvecZone();

    @Query("select b.etat, count(b) from Bac b group by b.etat")
    List<Object[]> compterParEtat();

    @Query("select coalesce(avg(b.niveauPourcent), 0) from Bac b")
    double niveauMoyen();
}
