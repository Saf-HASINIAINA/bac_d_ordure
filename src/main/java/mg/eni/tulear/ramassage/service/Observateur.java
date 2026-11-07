package mg.eni.tulear.ramassage.service;

import mg.eni.tulear.ramassage.domain.Alerte;

/** Pattern Observer : tout composant intéressé par les alertes l'implémente. */
public interface Observateur {
    void notifier(Alerte alerte);
}
