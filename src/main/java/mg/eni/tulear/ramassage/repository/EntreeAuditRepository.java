package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.EntreeAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface EntreeAuditRepository extends JpaRepository<EntreeAudit, Long> {

    /** Supprime les entrées antérieures à la date donnée ; renvoie leur nombre. */
    @Modifying
    @Query("delete from EntreeAudit e where e.dateAction < :limite")
    int supprimerAvant(@Param("limite") Instant limite);
}
