package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Chauffeur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChauffeurRepository extends JpaRepository<Chauffeur, Long> {
    List<Chauffeur> findByActifTrueOrderByNom();
}
