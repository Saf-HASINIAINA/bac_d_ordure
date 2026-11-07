package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Mesure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface MesureRepository extends JpaRepository<Mesure, Long> {

    /** Purge des anciennes mesures (la base gratuite de Supabase est limitée à 500 Mo). */
    @Modifying
    @Query("delete from Mesure m where m.horodatage < :limite")
    int supprimerAvant(@Param("limite") Instant limite);
}
