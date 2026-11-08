import { api } from '../api.js';
import { h, remplacer, decimal, insererDans } from '../dom.js';
import { executer, afficherMessage, marquerConnexion } from '../ui.js';
import { ouvrirFormulaire, confirmer } from '../formulaire.js';

export default {
  async monter(conteneur) {
    const corps = h('tbody');
    const champs = [
      { nom: 'nom', libelle: 'Nom de la zone (quartier)', type: 'texte', requis: true, maxlength: 100 },
      { nom: 'frequenceCollecteHeures', libelle: 'Fréquence de collecte (heures)', type: 'nombre', requis: true, min: 1, max: 720,
        aide: 'Ex. 24 pour une collecte quotidienne.' },
      { nom: 'position', libelle: 'Centre de la zone (facultatif)', type: 'position',
        aide: 'Sert à centrer la carte quand on installe un bac dans cette zone.' },
    ];
    const corpsZone = (v) => ({
      nom: v.nom, frequenceCollecteHeures: v.frequenceCollecteHeures,
      latitudeCentre: v.position?.latitude ?? null, longitudeCentre: v.position?.longitude ?? null,
    });

    const charger = async () => {
      try {
        const zones = await api.zones();
        remplacer(corps, zones.length === 0
          ? h('tr', {}, h('td', { colspan: 5, class: 'vide' }, 'Aucune zone : créez la première.'))
          : zones.map(ligne));
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    };

    const ajouter = async () => {
      const ok = await ouvrirFormulaire({
        titre: 'Ajouter une zone', libelleBouton: 'Ajouter', champs, valeurs: { frequenceCollecteHeures: 24 },
        soumettre: (v) => api.creerZone(corpsZone(v)),
      });
      if (ok) { afficherMessage('Zone ajoutée.'); await charger(); }
    };

    const modifier = async (z) => {
      const ok = await ouvrirFormulaire({
        titre: `Modifier la zone ${z.nom}`, champs,
        valeurs: { nom: z.nom, frequenceCollecteHeures: z.frequenceCollecteHeures,
          position: z.latitudeCentre != null ? { latitude: z.latitudeCentre, longitude: z.longitudeCentre } : null },
        soumettre: (v) => api.modifierZone(z.id, corpsZone(v)),
      });
      if (ok) { afficherMessage('Zone modifiée.'); await charger(); }
    };

    const supprimer = async (z) => {
      const ok = await confirmer({
        titre: `Supprimer la zone ${z.nom} ?`, danger: true, libelleBouton: 'Supprimer',
        texte: 'Une zone qui contient des bacs ou qui a un historique de tournées ne peut pas être supprimée.',
      });
      if (ok) await executer(() => api.supprimerZone(z.id), `Zone ${z.nom} supprimée.`, { apres: charger });
    };

    const ligne = (z) => h('tr', {},
      h('td', {}, h('strong', {}, z.nom)),
      h('td', {}, `toutes les ${z.frequenceCollecteHeures} h`),
      h('td', {}, z.latitudeCentre != null ? `${decimal(z.latitudeCentre)} ; ${decimal(z.longitudeCentre)}` : '–'),
      h('td', {}, String(z.nombreBacs)),
      h('td', { class: 'actions-ligne' },
        h('button', { class: 'bouton', type: 'button', onclick: () => modifier(z) }, 'Modifier'),
        h('button', { class: 'bouton danger', type: 'button', onclick: () => supprimer(z) }, 'Supprimer')));

    insererDans(conteneur, h('section', { class: 'bloc' },
      h('div', { class: 'barre-outils' }, h('span', { class: 'aide' }, 'Les zones regroupent les bacs par quartier ; une tournée se planifie zone par zone.'),
        h('button', { class: 'bouton principal pousse', type: 'button', onclick: ajouter }, '+ Ajouter une zone')),
      h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
        h('thead', {}, h('tr', {}, ['Zone', 'Collecte', 'Centre (lat. ; long.)', 'Bacs', 'Actions'].map((t) => h('th', {}, t)))),
        corps))));
    await charger();
  },
  demonter() {},
};
