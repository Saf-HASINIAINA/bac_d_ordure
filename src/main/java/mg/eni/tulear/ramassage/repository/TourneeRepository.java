package mg.eni.tulear.ramassage.repository;

import mg.eni.tulear.ramassage.domain.Tournee;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface TourneeRepository extends JpaRepository<Tournee, Long> {
    List<Tournee> findByStatut(StatutTournee statut);
    boolean existsByCamionIdAndStatutIn(Long camionId, Collection<StatutTournee> statuts);
    boolean existsByChauffeurIdAndStatutIn(Long chauffeurId, Collection<StatutTournee> statuts);
    List<Tournee> findAllByOrderByIdDesc();
    List<Tournee> findByStatutOrderByIdDesc(StatutTournee statut);
}
