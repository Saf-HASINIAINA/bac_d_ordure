package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Mesure;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.List;

/**
 * Insère un lot de mesures en un seul aller-retour réseau.
 * Hibernate ne sait pas grouper les insertions avec des colonnes IDENTITY, d'où JdbcTemplate.
 */
@Component
public class MesureBatchWriter {

    private static final String SQL =
            "insert into mesure (capteur_id, type, valeur, horodatage) values (?, ?, ?, ?)";

    private final JdbcTemplate jdbcTemplate;

    public MesureBatchWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void inserer(List<Mesure> mesures) {
        if (mesures.isEmpty()) return;
        jdbcTemplate.batchUpdate(SQL, mesures, 200, (ps, m) -> {
            ps.setLong(1, m.getCapteur().getId());
            ps.setString(2, m.getType().name());
            ps.setDouble(3, m.getValeur());
            ps.setTimestamp(4, Timestamp.from(m.getHorodatage()));
        });
    }
}
