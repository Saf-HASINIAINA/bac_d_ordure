package mg.eni.tulear.ramassage.securite;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limite les essais de connexion (contre le devinage de mots de passe) : après {@code maxEchecs} échecs
 * consécutifs pour une même adresse e-mail dans la fenêtre, toute connexion est refusée pendant {@code dureeBlocage},
 * même avec le bon mot de passe. Les adresses inconnues sont comptées aussi, pour ne pas révéler quels comptes existent.
 * État en mémoire (réinitialisé au redémarrage ; suffisant pour une seule instance).
 */
public class LimiteurConnexions {

    private static final int TAILLE_MAX_AVANT_NETTOYAGE = 10_000;

    private record Etat(int echecs, Instant premierEchec, Instant bloqueJusqua) { }

    private final int maxEchecs;
    private final Duration dureeBlocage;
    private final Clock horloge;
    private final Map<String, Etat> etats = new ConcurrentHashMap<>();

    public LimiteurConnexions(int maxEchecs, Duration dureeBlocage, Clock horloge) {
        if (maxEchecs < 1) throw new IllegalArgumentException("maxEchecs doit être >= 1");
        this.maxEchecs = maxEchecs;
        this.dureeBlocage = dureeBlocage;
        this.horloge = horloge;
    }

    /** @return la durée de blocage restante, ou {@link Duration#ZERO} si la connexion est permise */
    public Duration blocageRestant(String email) {
        Etat etat = etats.get(cle(email));
        if (etat == null || etat.bloqueJusqua() == null) return Duration.ZERO;
        Duration reste = Duration.between(horloge.instant(), etat.bloqueJusqua());
        return reste.isNegative() || reste.isZero() ? Duration.ZERO : reste;
    }

    public void enregistrerEchec(String email) {
        Instant maintenant = horloge.instant();
        if (etats.size() > TAILLE_MAX_AVANT_NETTOYAGE) nettoyer(maintenant);
        etats.compute(cle(email), (k, e) -> {
            if (e == null || expire(e, maintenant)) return nouvelEtat(1, maintenant);
            return nouvelEtat(e.echecs() + 1, e.premierEchec());
        });
    }

    public void enregistrerSucces(String email) {
        etats.remove(cle(email));
    }

    private Etat nouvelEtat(int echecs, Instant premierEchec) {
        Instant blocage = echecs >= maxEchecs ? horloge.instant().plus(dureeBlocage) : null;
        return new Etat(echecs, premierEchec, blocage);
    }

    /** Un état est périmé quand son blocage est fini, ou quand la fenêtre de comptage est écoulée sans blocage. */
    private boolean expire(Etat e, Instant maintenant) {
        if (e.bloqueJusqua() != null) return !maintenant.isBefore(e.bloqueJusqua());
        return !maintenant.isBefore(e.premierEchec().plus(dureeBlocage));
    }

    private void nettoyer(Instant maintenant) {
        etats.entrySet().removeIf(entree -> expire(entree.getValue(), maintenant));
    }

    private static String cle(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
