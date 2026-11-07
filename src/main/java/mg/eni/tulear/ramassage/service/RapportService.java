package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.api.dto.RapportDto;
import mg.eni.tulear.ramassage.domain.Bac;
import mg.eni.tulear.ramassage.domain.Collecte;
import mg.eni.tulear.ramassage.domain.Tournee;
import mg.eni.tulear.ramassage.domain.Zone;
import mg.eni.tulear.ramassage.domain.enums.StatutTournee;
import mg.eni.tulear.ramassage.domain.enums.TypeAlerte;
import mg.eni.tulear.ramassage.repository.AlerteRepository;
import mg.eni.tulear.ramassage.repository.CollecteRepository;
import mg.eni.tulear.ramassage.repository.TourneeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bilans d'activité. Tonnage et collectes : vidanges effectuées pendant la période (par heure de vidange).
 * Tournées et kilomètres : tournées terminées dont la date est dans la période.
 */
@Service
@Transactional(readOnly = true)
public class RapportService {

    static final int DUREE_MAX_JOURS = 366;
    static final int DUREE_PAR_DEFAUT_JOURS = 30;

    private final CollecteRepository collecteRepository;
    private final TourneeRepository tourneeRepository;
    private final AlerteRepository alerteRepository;
    private final ZoneId fuseau;

    public RapportService(CollecteRepository collecteRepository, TourneeRepository tourneeRepository,
                          AlerteRepository alerteRepository,
                          @Value("${app.fuseau:Indian/Antananarivo}") String fuseau) {
        this.collecteRepository = collecteRepository;
        this.tourneeRepository = tourneeRepository;
        this.alerteRepository = alerteRepository;
        this.fuseau = ZoneId.of(fuseau);
    }

    /** @param debut et @param fin facultatifs : par défaut les 30 derniers jours, aujourd'hui inclus. */
    public RapportDto generer(LocalDate debut, LocalDate fin) {
        LocalDate[] periode = periode(debut, fin);
        LocalDate d = periode[0];
        LocalDate f = periode[1];
        Instant de = d.atStartOfDay(fuseau).toInstant();
        Instant a = f.plusDays(1).atStartOfDay(fuseau).toInstant();

        List<Collecte> collectes = collecteRepository.findEffectueesEntre(de, a);
        List<Tournee> tournees = tourneeRepository.findByStatutAndDateBetween(StatutTournee.TERMINEE, d, f);

        double tonnage = collectes.stream().mapToDouble(Collecte::getPoidsCollecteKg).sum();
        double km = tournees.stream().mapToDouble(Tournee::getDistanceKm).sum();
        double remplissage = collectes.stream().mapToDouble(RapportService::remplissagePourcent).average().orElse(0);

        return new RapportDto(d, f, tonnage, tournees.size(), collectes.size(), km, remplissage,
                alertesParType(de, a), parZone(collectes, tournees), parJour(collectes, d, f));
    }

    /** Export CSV : une ligne par vidange. */
    public String exporterCsv(LocalDate debut, LocalDate fin) {
        LocalDate[] periode = periode(debut, fin);
        Instant de = periode[0].atStartOfDay(fuseau).toInstant();
        Instant a = periode[1].plusDays(1).atStartOfDay(fuseau).toInstant();
        return CsvCollectes.generer(collecteRepository.findEffectueesEntre(de, a), fuseau);
    }

    /** Nom de fichier proposé pour l'export. */
    public String nomFichier(LocalDate debut, LocalDate fin) {
        LocalDate[] periode = periode(debut, fin);
        return "rapport-collectes-%s_%s.csv".formatted(periode[0], periode[1]);
    }

    LocalDate[] periode(LocalDate debut, LocalDate fin) {
        LocalDate f = fin != null ? fin : LocalDate.now(fuseau);
        LocalDate d = debut != null ? debut : f.minusDays(DUREE_PAR_DEFAUT_JOURS - 1L);
        if (d.isAfter(f)) {
            throw new RequeteInvalideException("La date de début doit précéder la date de fin");
        }
        if (ChronoUnit.DAYS.between(d, f) >= DUREE_MAX_JOURS) {
            throw new RequeteInvalideException("La période ne peut pas dépasser " + DUREE_MAX_JOURS + " jours");
        }
        return new LocalDate[] {d, f};
    }

    /** Remplissage du bac au moment de la vidange, déduit du poids collecté (plafonné à 100 %). */
    static double remplissagePourcent(Collecte c) {
        Bac bac = c.getBac();
        double poidsPlein = bac.getCapaciteLitres() * Bac.DENSITE_KG_PAR_LITRE;
        return poidsPlein <= 0 ? 0 : Math.min(100, c.getPoidsCollecteKg() / poidsPlein * 100);
    }

    private Map<TypeAlerte, Long> alertesParType(Instant de, Instant a) {
        Map<TypeAlerte, Long> parType = new EnumMap<>(TypeAlerte.class);
        for (TypeAlerte type : TypeAlerte.values()) parType.put(type, 0L);
        for (Object[] ligne : alerteRepository.compterParType(de, a)) {
            parType.put((TypeAlerte) ligne[0], (Long) ligne[1]);
        }
        return parType;
    }

    private List<RapportDto.ParZone> parZone(List<Collecte> collectes, List<Tournee> tournees) {
        Map<Long, Zone> zones = new LinkedHashMap<>();
        Map<Long, double[]> cumul = new LinkedHashMap<>(); // [tonnage, collectes, tournées, km]
        for (Collecte c : collectes) {
            Zone z = c.getBac().getZone();
            zones.putIfAbsent(z.getId(), z);
            double[] t = cumul.computeIfAbsent(z.getId(), k -> new double[4]);
            t[0] += c.getPoidsCollecteKg();
            t[1] += 1;
        }
        for (Tournee tournee : tournees) {
            Zone z = tournee.getZone();
            zones.putIfAbsent(z.getId(), z);
            double[] t = cumul.computeIfAbsent(z.getId(), k -> new double[4]);
            t[2] += 1;
            t[3] += tournee.getDistanceKm();
        }
        return cumul.entrySet().stream()
                .map(e -> new RapportDto.ParZone(e.getKey(), zones.get(e.getKey()).getNom(),
                        e.getValue()[0], (int) e.getValue()[1], (int) e.getValue()[2], e.getValue()[3]))
                .sorted(Comparator.comparingDouble(RapportDto.ParZone::tonnageKg).reversed())
                .collect(Collectors.toList());
    }

    /** Un point par jour de la période, y compris les jours sans collecte (tonnage 0). */
    private List<RapportDto.ParJour> parJour(List<Collecte> collectes, LocalDate debut, LocalDate fin) {
        Map<LocalDate, double[]> jours = new TreeMap<>();
        for (LocalDate j = debut; !j.isAfter(fin); j = j.plusDays(1)) jours.put(j, new double[2]);
        for (Collecte c : collectes) {
            double[] t = jours.get(c.getHeureCollecte().atZone(fuseau).toLocalDate());
            if (t != null) {
                t[0] += c.getPoidsCollecteKg();
                t[1] += 1;
            }
        }
        return jours.entrySet().stream()
                .map(e -> new RapportDto.ParJour(e.getKey(), e.getValue()[0], (int) e.getValue()[1]))
                .collect(Collectors.toList());
    }
}
