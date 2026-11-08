import { api } from '../api.js';
import { h, remplacer, dateHeure, insererDans } from '../dom.js';
import { boucle, detecteurChangement, marquerConnexion, boutonAction, badge } from '../ui.js';
import { LIBELLES_ALERTE } from '../libelles.js';

let courant = null;

export default {
  async monter(conteneur) {
    const changement = detecteurChangement();
    let controle = null;
    const filtre = h('select', { 'aria-label': 'Filtrer les alertes' },
      h('option', { value: 'false' }, 'À traiter'), h('option', { value: 'true' }, 'Déjà traitées'), h('option', { value: '' }, 'Toutes'));
    const compte = h('span', { class: 'aide' });
    const corps = h('tbody');

    const ligne = (a) => h('tr', {},
      h('td', {}, dateHeure(a.dateCreation)),
      h('td', {}, badge(a.type, LIBELLES_ALERTE[a.type] ?? a.type)),
      h('td', { class: 'texte-long' }, a.message),
      h('td', {}, a.bacCode ?? '–'),
      h('td', { class: 'actions-ligne' }, a.traitee
        ? h('span', { class: 'aide' }, 'Traitée')
        : boutonAction('Marquer comme traitée', () => api.traiterAlerte(a.id), 'Alerte traitée.', { apres: () => controle.rafraichir() })));

    const rafraichir = async () => {
      try {
        const alertes = await api.alertes(filtre.value === '' ? undefined : filtre.value === 'true');
        compte.textContent = `${alertes.length} alerte(s)`;
        if (changement('alertes', alertes)) {
          remplacer(corps, alertes.length === 0
            ? h('tr', {}, h('td', { colspan: 5, class: 'vide' }, filtre.value === 'false' ? 'Aucune alerte à traiter.' : 'Aucune alerte.'))
            : alertes.slice(0, 200).map(ligne));
        }
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    };
    filtre.addEventListener('change', () => controle.rafraichir());

    insererDans(conteneur, h('section', { class: 'bloc' },
      h('div', { class: 'barre-outils' }, filtre, compte,
        h('span', { class: 'aide pousse' }, 'Une alerte de bac plein se ferme toute seule quand le bac est vidé.')),
      h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
        h('thead', {}, h('tr', {}, ['Date', 'Type', 'Message', 'Bac', 'Action'].map((t) => h('th', {}, t)))), corps))));

    controle = boucle(rafraichir, 5000);
    courant = controle;
  },
  demonter() { courant?.arreter(); courant = null; },
};
