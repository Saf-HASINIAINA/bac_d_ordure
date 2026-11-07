package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Collecte;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CollecteRepository extends JpaRepository<Collecte, Long> {

    /** Bacs déjà prévus dans une tournée de ces statuts et pas encore vidés. */
    @Query("select c.bac.id from Collecte c where c.tournee.statut in :statuts and c.effectuee = false")
    List<Long> findBacIdsEnTournee(@Param("statuts") Collection<StatutTournee> statuts);

    @Query("select coalesce(sum(c.poidsCollecteKg), 0) from Collecte c where c.effectuee = true")
    double tonnageCollecteKg();
}
