package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Capteur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CapteurRepository extends JpaRepository<Capteur, Long> {

    List<Capteur> findByActifTrue();

    /** Charge les capteurs actifs avec leur bac en une seule requête (évite le problème N+1). */
    @Query("select c from Capteur c join fetch c.bac where c.actif = true")
    List<Capteur> findActifsAvecBac();
}
