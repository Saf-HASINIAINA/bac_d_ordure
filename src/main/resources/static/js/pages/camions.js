import { api } from '../api.js';
import { h, remplacer, entier, insererDans } from '../dom.js';
import { estAdmin } from '../etat.js';
import { boucle, marquerConnexion, boutonAction, badge, executer, afficherMessage } from '../ui.js';
import { LIBELLES_CAMION } from '../libelles.js';
import { ouvrirFormulaire, confirmer } from '../formulaire.js';

let courant = null;

export default {
  async monter(conteneur) {
    const admin = estAdmin();
    let controle = null;
    const corps = h('tbody');

    const champs = [
      { nom: 'immatriculation', libelle: 'Immatriculation', type: 'texte', requis: true, maxlength: 30, placeholder: '1234 TAA' },
      { nom: 'capaciteKg', libelle: 'Capacité de charge (kg)', type: 'nombre', requis: true, min: 100, max: 50000, step: 'any' },
    ];

    const ajouter = async () => {
      let cree = null;
      const ok = await ouvrirFormulaire({
        titre: 'Ajouter un camion', libelleBouton: 'Ajouter', champs, valeurs: { capaciteKg: 5000 },
        soumettre: async (v) => { cree = await api.creerCamion({ immatriculation: v.immatriculation, capaciteKg: v.capaciteKg }); },
      });
      if (ok) { afficherMessage(`Camion ${cree.immatriculation} ajouté : il est au dépôt, disponible.`); await controle.rafraichir(); }
    };

    const modifier = async (c) => {
      const ok = await ouvrirFormulaire({
        titre: `Modifier le camion ${c.immatriculation}`, champs,
        valeurs: { immatriculation: c.immatriculation, capaciteKg: c.capaciteKg },
        soumettre: (v) => api.modifierCamion(c.id, { immatriculation: v.immatriculation, capaciteKg: v.capaciteKg }),
      });
      if (ok) { afficherMessage('Camion modifié.'); await controle.rafraichir(); }
    };

    const supprimer = async (c) => {
      const ok = await confirmer({
        titre: `Supprimer le camion ${c.immatriculation} ?`, danger: true, libelleBouton: 'Supprimer',
        texte: 'Un camion qui a déjà effectué des tournées ne peut pas être supprimé : déclarez-le en panne à la place.',
      });
      if (ok) await executer(() => api.supprimerCamion(c.id), `Camion ${c.immatriculation} supprimé.`, { apres: () => controle.rafraichir() });
    };

    const ligne = (c) => {
      const apres = () => controle.rafraichir();
      const actions = [];
      if (c.statut === 'DISPONIBLE') {
        actions.push(boutonAction('Déclarer en panne', () => api.camionEnPanne(c.id), `Camion ${c.immatriculation} déclaré en panne.`, { apres }));
      } else if (c.statut === 'EN_PANNE') {
        actions.push(boutonAction('Remettre à disposition', () => api.camionDisponible(c.id), `Camion ${c.immatriculation} remis à disposition.`, { apres }));
      }
      if (admin) {
        actions.push(h('button', { class: 'bouton', type: 'button', onclick: () => modifier(c) }, 'Modifier'));
        actions.push(h('button', { class: 'bouton danger', type: 'button', onclick: () => supprimer(c) }, 'Supprimer'));
      }
      const classeStatut = { DISPONIBLE: 'TERMINEE', EN_TOURNEE: 'EN_COURS', EN_PANNE: 'CRITIQUE' }[c.statut];
      return h('tr', {},
        h('td', {}, h('strong', {}, c.immatriculation)),
        h('td', {}, `${entier(c.capaciteKg)} kg`),
        h('td', {}, `${entier(c.chargeActuelleKg)} kg`),
        h('td', {}, badge(classeStatut, LIBELLES_CAMION[c.statut] ?? c.statut)),
        h('td', { class: 'actions-ligne' }, actions));
    };

    insererDans(conteneur, h('section', { class: 'bloc' },
      h('div', { class: 'barre-outils' }, h('span', { class: 'aide' }, 'Un camion en panne n\'est plus proposé pour les tournées.'),
        admin ? h('button', { class: 'bouton principal pousse', type: 'button', onclick: ajouter }, '+ Ajouter un camion') : null),
      h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
        h('thead', {}, h('tr', {}, ['Immatriculation', 'Capacité', 'Charge actuelle', 'Statut', 'Actions'].map((t) => h('th', {}, t)))),
        corps))));

    controle = boucle(async () => {
      try {
        const camions = await api.camions();
        remplacer(corps, camions.length === 0
          ? h('tr', {}, h('td', { colspan: 5, class: 'vide' }, 'Aucun camion enregistré.'))
          : camions.map(ligne));
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    }, 10000);
    courant = controle;
  },

  demonter() { courant?.arreter(); courant = null; },
};
