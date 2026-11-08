import { etat, peutGerer, estAdmin, estChauffeur } from './etat.js';
import { afficherMessage } from './ui.js';

const GERANTS = () => peutGerer();
const ADMIN = () => estAdmin();
const TOUS = () => true;

/**
 * Pages de l'application. `visible(role)` décide à la fois de la présence dans le menu et de l'accès :
 * taper l'adresse d'une page interdite renvoie au tableau de bord. (Le serveur refuse de toute façon.)
 */
export const PAGES = [
  { id: 'tableau-de-bord', titre: 'Tableau de bord', icone: '🗺️', groupe: 'Exploitation', visible: TOUS,
    charger: () => import('./pages/tableau.js') },
  { id: 'tournees', titre: () => (estChauffeur() ? 'Mes tournées' : 'Tournées'), icone: '🚛', groupe: 'Exploitation',
    visible: TOUS, charger: () => import('./pages/tournees.js') },
  { id: 'alertes', titre: 'Alertes', icone: '🔔', groupe: 'Exploitation', visible: GERANTS,
    charger: () => import('./pages/alertes.js') },
  { id: 'bacs', titre: 'Bacs', icone: '🗑️', groupe: 'Parc', visible: GERANTS, charger: () => import('./pages/bacs.js') },
  { id: 'camions', titre: 'Camions', icone: '🚚', groupe: 'Parc', visible: GERANTS,
    charger: () => import('./pages/camions.js') },
  { id: 'zones', titre: 'Zones', icone: '📍', groupe: 'Parc', visible: ADMIN, charger: () => import('./pages/zones.js') },
  { id: 'rapports', titre: "Rapports", icone: '📊', groupe: 'Analyse', visible: GERANTS,
    charger: () => import('./pages/rapports.js') },
  { id: 'utilisateurs', titre: 'Utilisateurs', icone: '👥', groupe: 'Administration', visible: ADMIN,
    charger: () => import('./pages/utilisateurs.js') },
  { id: 'compte', titre: 'Mon compte', icone: '👤', groupe: null, visible: TOUS, charger: () => import('./pages/compte.js') },
];

const PAGE_PAR_DEFAUT = 'tableau-de-bord';
const titreDe = (page) => (typeof page.titre === 'function' ? page.titre() : page.titre);

let pageCourante = null;
let numeroNavigation = 0;

export const pagesAccessibles = () => PAGES.filter((p) => p.visible());

function identifiantDemande() {
  return location.hash.replace(/^#\/?/, '') || PAGE_PAR_DEFAUT;
}

/** Démarre la navigation : réagit au changement d'adresse (#/bacs, #/zones...). */
export function demarrerRouteur({ conteneur, titre, surChangement }) {
  async function aller() {
    const numero = ++numeroNavigation;
    let page = PAGES.find((p) => p.id === identifiantDemande());
    if (!page || !page.visible()) {
      if (page) afficherMessage("Cette page n'est pas accessible avec votre rôle.", 'erreur');
      location.replace(`#/${PAGE_PAR_DEFAUT}`);
      return; // le changement d'adresse relance aller()
    }

    if (pageCourante) {
      try { pageCourante.module.demonter?.(); } catch (e) { console.error(e); }
      pageCourante = null;
    }
    conteneur.replaceChildren();
    titre.textContent = titreDe(page);
    document.title = `${titreDe(page)} - Ramassage des ordures`;
    surChangement(page);

    try {
      const module = (await page.charger()).default;
      if (numero !== numeroNavigation) return; // une autre navigation a démarré entre-temps
      pageCourante = { page, module };
      await module.monter(conteneur);
      conteneur.focus({ preventScroll: true });
      window.scrollTo(0, 0);
    } catch (e) {
      console.error(e);
      if (numero === numeroNavigation) afficherMessage(`Impossible d'afficher cette page : ${e.message}`, 'erreur');
    }
  }
  window.addEventListener('hashchange', aller);
  return aller();
}

export function demonterPageCourante() {
  if (pageCourante) {
    try { pageCourante.module.demonter?.(); } catch (e) { console.error(e); }
    pageCourante = null;
  }
}
