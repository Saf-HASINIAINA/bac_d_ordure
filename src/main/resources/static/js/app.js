import { api } from './api.js';
import { h, remplacer } from './dom.js';
import { etat, peutGerer } from './etat.js';
import { LIBELLES_ROLE } from './libelles.js';
import { demarrerRouteur, pagesAccessibles } from './routeur.js';
import { afficherMessage, marquerConnexion, boucle } from './ui.js';

const $ = (id) => document.getElementById(id);
const titreDe = (page) => (typeof page.titre === 'function' ? page.titre() : page.titre);

/** Menu latéral : uniquement les pages permises au rôle, regroupées par thème. */
function construireMenu() {
  const groupes = [];
  for (const page of pagesAccessibles().filter((p) => p.groupe)) {
    let groupe = groupes.find((g) => g.nom === page.groupe);
    if (!groupe) {
      groupe = { nom: page.groupe, pages: [] };
      groupes.push(groupe);
    }
    groupe.pages.push(page);
  }
  remplacer($('nav'), groupes.map((g) => h('div', { class: 'barre-groupe' },
    h('p', { class: 'barre-titre' }, g.nom),
    g.pages.map((p) => h('a', { class: 'barre-lien', href: `#/${p.id}`, 'data-page': p.id },
      h('span', { class: 'icone', 'aria-hidden': 'true' }, p.icone),
      h('span', { class: 'barre-texte' }, titreDe(p)),
      p.id === 'alertes' ? h('span', { class: 'compteur', id: 'nav-alertes', hidden: true }, '0') : null)))));
}

function afficherUtilisateurConnecte() {
  $('utilisateur-nom').textContent = `${etat.moi.prenom} ${etat.moi.nom}`;
  $('utilisateur-role').textContent = LIBELLES_ROLE[etat.moi.role] ?? etat.moi.role;
  $('barre-pied').hidden = false;
  $('bouton-deconnexion').addEventListener('click', async (e) => {
    e.currentTarget.disabled = true;
    try { await api.deconnexion(); } catch { /* même en cas d'échec réseau, on repart sur la page de connexion */ }
    location.replace('/login.html');
  });
}

/* ---------- menu sur petit écran ---------- */

function brancherMenuMobile() {
  const barre = $('barre');
  const voile = $('voile');
  const bouton = $('menu-bouton');
  const ouvrir = (ouvert) => {
    barre.classList.toggle('ouverte', ouvert);
    voile.hidden = !ouvert;
    bouton.setAttribute('aria-expanded', String(ouvert));
  };
  bouton.addEventListener('click', () => ouvrir(!barre.classList.contains('ouverte')));
  voile.addEventListener('click', () => ouvrir(false));
  document.addEventListener('keydown', (e) => { if (e.key === 'Escape') ouvrir(false); });
  return () => ouvrir(false);
}

/** Pastille du nombre d'alertes à traiter, à côté du menu « Alertes ». */
function suivreAlertes() {
  boucle(async () => {
    try {
      const alertes = await api.alertes(false);
      const pastille = $('nav-alertes');
      if (!pastille) return;
      pastille.textContent = alertes.length;
      pastille.hidden = alertes.length === 0;
    } catch { /* l'indicateur de connexion de la page signale déjà les pannes */ }
  }, 15000);
}

async function demarrer() {
  try {
    etat.moi = await api.moi(); // sans session, api.js renvoie vers la page de connexion
    etat.configuration = await api.configuration();
  } catch (e) {
    if (e.statut !== 401) {
      marquerConnexion(false);
      afficherMessage("Impossible de joindre le serveur. Vérifiez que l'application est démarrée.", 'erreur');
    }
    return;
  }
  construireMenu();
  afficherUtilisateurConnecte();
  const fermerMenu = brancherMenuMobile();
  if (peutGerer()) suivreAlertes();

  await demarrerRouteur({
    conteneur: $('page'),
    titre: $('titre-page'),
    surChangement: (page) => {
      for (const lien of document.querySelectorAll('[data-page]')) {
        if (lien.dataset.page === page.id) lien.setAttribute('aria-current', 'page');
        else lien.removeAttribute('aria-current');
      }
      fermerMenu();
    },
  });
}

demarrer();
