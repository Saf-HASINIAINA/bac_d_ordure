import { h } from './dom.js';

/* ---------- messages ---------- */

let minuteurMessage;
/** Message en haut de page (succès ou erreur), effacé au bout de 7 secondes. */
export function afficherMessage(texte, type = 'succes') {
  const zone = document.getElementById('message');
  zone.textContent = texte;
  zone.className = `message ${type}`;
  clearTimeout(minuteurMessage);
  if (texte) minuteurMessage = setTimeout(() => afficherMessage('', ''), 7000);
}

export function marquerConnexion(enLigne) {
  document.getElementById('connexion').className = `connexion ${enLigne ? 'en-ligne' : 'hors-ligne'}`;
  document.getElementById('connexion-texte').textContent = enLigne
    ? `En direct · ${new Date().toLocaleTimeString('fr-FR')}`
    : 'Serveur injoignable · nouvelle tentative…';
}

/* ---------- actions ---------- */

/**
 * Exécute une action de l'API, affiche le résultat, puis appelle `apres` (rafraîchissement de la page).
 * @returns true si l'action a réussi
 */
export async function executer(action, messageSucces, { boutons = [], apres } = {}) {
  boutons.forEach((b) => { if (b) b.disabled = true; });
  try {
    const resultat = await action();
    if (messageSucces) afficherMessage(messageSucces, 'succes');
    if (apres) await apres(resultat);
    return true;
  } catch (e) {
    if (e.statut !== 401) afficherMessage(e.message, 'erreur');
    return false;
  } finally {
    boutons.forEach((b) => { if (b) b.disabled = false; });
  }
}

export function boutonAction(libelle, action, messageSucces, { classe = '', apres } = {}) {
  const bouton = h('button', { class: `bouton ${classe}`.trim(), type: 'button' }, libelle);
  bouton.addEventListener('click', () => executer(action, messageSucces, { boutons: [bouton], apres }));
  return bouton;
}

/* ---------- boucles de rafraîchissement ---------- */

/**
 * Appelle `fonction` tout de suite puis toutes les `ms` millisecondes, sans chevauchement et en pause
 * quand l'onglet est caché. `rafraichir()` demande une actualisation immédiate : si une est déjà en cours,
 * une nouvelle démarre juste après (sinon la liste n'afficherait le résultat d'une action qu'au tour suivant).
 * `arreter()` doit être appelé quand on quitte la page.
 */
export function boucle(fonction, ms) {
  let enCours = false;
  let relancer = false;
  let arretee = false;
  const tour = async () => {
    if (arretee) return;
    if (enCours) { relancer = true; return; }
    enCours = true;
    try {
      do {
        relancer = false;
        await fonction();
      } while (relancer && !arretee);
    } finally {
      enCours = false;
    }
  };
  const minuteur = setInterval(() => { if (!document.hidden) tour(); }, ms);
  const retour = () => { if (!document.hidden) tour(); };
  document.addEventListener('visibilitychange', retour);
  tour();
  return {
    rafraichir: tour,
    arreter() {
      arretee = true;
      clearInterval(minuteur);
      document.removeEventListener('visibilitychange', retour);
    },
  };
}

/** Évite de reconstruire une zone dont les données n'ont pas changé (garde le focus et les clics). */
export function detecteurChangement() {
  const signatures = {};
  return (cle, donnees) => {
    const signature = JSON.stringify(donnees);
    if (signatures[cle] === signature) return false;
    signatures[cle] = signature;
    return true;
  };
}

/* ---------- petits éléments communs ---------- */

export function badge(classe, texte) {
  return h('span', { class: `badge ${classe}` }, texte);
}

export function barreNiveau(pourcent, couleur) {
  return h('span', { class: 'niveau', role: 'img', 'aria-label': `${Math.round(pourcent)} %` },
    h('span', { style: `width:${Math.min(100, Math.max(0, pourcent))}%;background:${couleur}` }));
}

export function titreSection(texte, ...complements) {
  return h('div', { class: 'section-tete' }, h('h2', {}, texte), ...complements);
}
