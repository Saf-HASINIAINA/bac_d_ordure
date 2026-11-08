import { api } from '../api.js';
import { h, remplacer, entier, decimal, insererDans } from '../dom.js';
import { etat, peutGerer } from '../etat.js';
import { boucle, detecteurChangement, marquerConnexion, executer, boutonAction, badge, afficherMessage } from '../ui.js';
import { LIBELLES_TOURNEE } from '../libelles.js';
import { ouvrirFormulaire } from '../formulaire.js';

const NB_MAX = 30;
let courant = null;

function remplirSelect(select, options, vide) {
  const valeur = select.value;
  remplacer(select, options.length === 0
    ? h('option', { value: '' }, vide)
    : options.map((o) => h('option', { value: o.valeur }, o.libelle)));
  if (options.some((o) => String(o.valeur) === valeur)) select.value = valeur;
}

export default {
  async monter(conteneur) {
    const gerer = peutGerer();
    const changement = detecteurChangement();
    let tournees = [];
    let controle = null;

    const liste = h('div', { class: 'tournees' });
    const filtre = h('select', { 'aria-label': 'Filtrer par statut' },
      h('option', { value: '' }, 'Toutes les tournées'),
      Object.entries(LIBELLES_TOURNEE).map(([valeur, libelle]) => h('option', { value: valeur }, libelle)));
    const choixZone = h('select', { required: true, 'aria-label': 'Zone' });
    const choixCamion = h('select', { required: true, 'aria-label': 'Camion' });
    const choixChauffeur = h('select', { required: true, 'aria-label': 'Chauffeur' });
    const formulaire = h('form', { class: 'formulaire' },
      h('label', {}, 'Zone', choixZone), h('label', {}, 'Camion disponible', choixCamion),
      h('label', {}, 'Chauffeur', choixChauffeur),
      h('button', { type: 'submit', class: 'bouton principal' }, 'Planifier'));

    insererDans(conteneur, 
      gerer ? h('section', { class: 'bloc' }, h('h2', {}, 'Planifier une tournée'), formulaire,
        h('p', { class: 'aide' }, "Les bacs remplis à 80 % ou plus sont retenus, les plus pleins d'abord, dans la limite de la capacité du camion.")) : null,
      h('section', { class: 'bloc' },
        h('div', { class: 'section-tete' }, h('h2', {}, gerer ? 'Tournées' : 'Mes tournées'), filtre), liste));

    if (gerer) {
      const [zones, chauffeurs] = await Promise.all([api.zones(), api.chauffeurs()]);
      remplirSelect(choixZone, zones.map((z) => ({ valeur: z.id, libelle: `${z.nom} (${z.nombreBacs} bacs)` })), 'Aucune zone');
      remplirSelect(choixChauffeur, chauffeurs.map((c) => ({ valeur: c.id, libelle: `${c.prenom} ${c.nom}` })), 'Aucun chauffeur');
      formulaire.addEventListener('submit', (evenement) => {
        evenement.preventDefault();
        executer(async () => {
          const t = await api.planifier({
            zoneId: Number(choixZone.value), camionId: Number(choixCamion.value), chauffeurId: Number(choixChauffeur.value),
          });
          etat.tourneeChoisie = t.id;
        }, 'Tournée planifiée : elle est tracée en pointillés sur la carte du tableau de bord.',
        { boutons: [formulaire.querySelector('button')], apres: () => controle.rafraichir() });
      });
    }

    const signalerIncident = async (t) => {
      const ouverte = await ouvrirFormulaire({
        titre: `Signaler un incident - tournée n°${t.id}`,
        libelleBouton: 'Signaler',
        champs: [
          { nom: 'bacId', libelle: 'Bac concerné (facultatif)', type: 'choix', options: [
            { valeur: '', libelle: 'Aucun bac en particulier' },
            ...[...t.collectes].sort((a, b) => a.ordre - b.ordre).map((c) => ({ valeur: c.bacId, libelle: `${c.ordre}. ${c.bacCode}` }))] },
          { nom: 'description', libelle: "Que s'est-il passé ?", type: 'zonetexte', requis: true, minlength: 3, maxlength: 300,
            placeholder: 'Ex. accès à la rue bloqué, bac cassé, camion en difficulté…' },
        ],
        soumettre: (v) => api.signalerIncident(t.id, { description: v.description, bacId: v.bacId ? Number(v.bacId) : null }),
      });
      if (ouverte) afficherMessage('Incident signalé : le superviseur est prévenu.', 'succes');
    };

    const carteTournee = (t) => {
      const faites = t.collectes.filter((c) => c.effectuee).length;
      const apres = () => controle.rafraichir();
      const lignes = [...t.collectes].sort((a, b) => a.ordre - b.ordre).map((c) => {
        const contenu = [h('span', {}, `${c.ordre}. ${c.bacCode} · ${entier(c.niveauPourcent)} %`)];
        if (c.effectuee) contenu.push(h('span', {}, `${entier(c.poidsCollecteKg)} kg ✓`));
        else if (t.statut === 'EN_COURS') {
          contenu.push(boutonAction('Confirmer la vidange', () => api.confirmerCollecte(t.id, c.bacId), `Bac ${c.bacCode} vidé.`, { apres }));
        }
        return h('li', { class: `collecte${c.effectuee ? ' faite' : ''}` }, contenu);
      });

      const actions = [h('button', { class: 'bouton', type: 'button',
        onclick: () => { etat.tourneeChoisie = t.id; location.hash = '#/tableau-de-bord'; } }, 'Voir sur la carte')];
      if (t.statut === 'PLANIFIEE') {
        actions.push(boutonAction('Démarrer', () => api.demarrer(t.id), `Tournée n°${t.id} démarrée.`, { classe: 'principal', apres }));
        if (gerer) actions.push(boutonAction('Annuler', () => api.annuler(t.id), `Tournée n°${t.id} annulée.`, { classe: 'danger', apres }));
      }
      if (t.statut === 'EN_COURS') {
        actions.push(boutonAction('Terminer', () => api.terminer(t.id), `Tournée n°${t.id} terminée.`, { classe: 'principal', apres }));
      }
      if (t.statut === 'PLANIFIEE' || t.statut === 'EN_COURS') {
        actions.push(h('button', { class: 'bouton', type: 'button', onclick: () => signalerIncident(t) }, '⚠ Signaler un incident'));
      }

      return h('article', { class: `tournee${t.id === etat.tourneeChoisie ? ' selection' : ''}` },
        h('div', { class: 'tournee-tete' }, h('strong', {}, `Tournée n°${t.id} · ${t.zoneNom}`), badge(t.statut, LIBELLES_TOURNEE[t.statut] ?? t.statut)),
        h('div', { class: 'tournee-meta' }, `${t.camionImmatriculation} · ${t.chauffeurNom} · ${decimal(t.distanceKm)} km · ${t.date}`),
        h('progress', { max: t.collectes.length, value: faites, 'aria-label': 'Bacs vidés' }),
        h('div', { class: 'tournee-meta' },
          `${faites} / ${t.collectes.length} bacs vidés · collecté ${entier(t.poidsCollecteKg)} kg · attendu ${entier(t.poidsAttenduKg)} kg`),
        h('ul', { class: 'collectes' }, lignes),
        h('div', { class: 'actions' }, actions));
    };

    const afficherListe = (force = false) => {
      const actives = tournees.filter((t) => t.statut === 'EN_COURS' || t.statut === 'PLANIFIEE');
      const autres = tournees.filter((t) => !actives.includes(t));
      const visibles = [...actives, ...autres].filter((t) => !filtre.value || t.statut === filtre.value).slice(0, NB_MAX);
      if (!force && !changement('liste', { visibles, choisie: etat.tourneeChoisie })) return;
      remplacer(liste, visibles.length === 0
        ? h('p', { class: 'vide' }, gerer ? 'Aucune tournée pour le moment. Planifiez-en une ci-dessus.' : "Aucune tournée ne vous est confiée pour le moment.")
        : visibles.map(carteTournee));
    };
    filtre.addEventListener('change', () => afficherListe(true));

    const rafraichir = async () => {
      try {
        const [liste2, camions] = await Promise.all([api.tournees(), gerer ? api.camions() : null]);
        tournees = liste2;
        if (camions && changement('camions', camions.map((c) => [c.id, c.statut]))) {
          remplirSelect(choixCamion, camions.filter((c) => c.statut === 'DISPONIBLE')
            .map((c) => ({ valeur: c.id, libelle: `${c.immatriculation} (${entier(c.capaciteKg)} kg)` })), 'Aucun camion disponible');
        }
        afficherListe();
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    };
    controle = boucle(rafraichir, 5000);
    courant = controle;
  },

  demonter() {
    courant?.arreter();
    courant = null;
  },
};
