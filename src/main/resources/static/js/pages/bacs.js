import { api } from '../api.js';
import { h, remplacer, entier, decimal, dateHeure, insererDans } from '../dom.js';
import { estAdmin } from '../etat.js';
import { boucle, marquerConnexion, boutonAction, badge, barreNiveau, executer, afficherMessage } from '../ui.js';
import { COULEURS, LIBELLES_ETAT, LIBELLES_DECHET } from '../libelles.js';
import { ouvrirFormulaire, confirmer } from '../formulaire.js';

let courant = null;

const options = (dictionnaire) => Object.entries(dictionnaire).map(([valeur, libelle]) => ({ valeur, libelle }));

export default {
  async monter(conteneur) {
    const admin = estAdmin();
    let bacs = [];
    const zones = await api.zones();
    let controle = null;

    const choixZone = h('select', { 'aria-label': 'Filtrer par zone' }, h('option', { value: '' }, 'Toutes les zones'),
      zones.map((z) => h('option', { value: z.id }, z.nom)));
    const choixEtat = h('select', { 'aria-label': 'Filtrer par état' }, h('option', { value: '' }, 'Tous les états'),
      options(LIBELLES_ETAT).map((o) => h('option', { value: o.valeur }, o.libelle)));
    const recherche = h('input', { type: 'search', placeholder: 'Rechercher un code…', 'aria-label': 'Rechercher un bac' });
    const compte = h('span', { class: 'aide' });
    const corps = h('tbody');

    const champsBac = (creation) => [
      creation ? { nom: 'code', libelle: 'Code du bac', type: 'texte', maxlength: 50,
        aide: 'Facultatif : laissé vide, un code est généré (BAC-025…).' } : null,
      { nom: 'zoneId', libelle: 'Zone', type: 'choix', requis: true, options: zones.map((z) => ({ valeur: z.id, libelle: z.nom })) },
      { nom: 'position', libelle: 'Emplacement du bac', type: 'position', requis: true,
        centre: zones.find((z) => z.latitudeCentre != null)
          ? { latitude: zones.find((z) => z.latitudeCentre != null).latitudeCentre, longitude: zones.find((z) => z.latitudeCentre != null).longitudeCentre }
          : undefined },
      { nom: 'capaciteLitres', libelle: 'Capacité (litres)', type: 'nombre', requis: true, min: 10, max: 10000, step: 'any' },
      { nom: 'typeDechet', libelle: 'Type de déchet', type: 'choix', options: options(LIBELLES_DECHET) },
    ].filter(Boolean);

    const corpsBac = (v) => ({
      code: v.code || null, zoneId: Number(v.zoneId), latitude: v.position.latitude, longitude: v.position.longitude,
      capaciteLitres: v.capaciteLitres, typeDechet: v.typeDechet || null,
    });

    const installer = async () => {
      if (zones.length === 0) { afficherMessage("Créez d'abord une zone (menu Zones).", 'erreur'); return; }
      let cree = null;
      const ok = await ouvrirFormulaire({
        titre: 'Installer un bac', libelleBouton: 'Installer le bac', champs: champsBac(true),
        valeurs: { zoneId: zones[0].id, capaciteLitres: 240, typeDechet: 'MENAGER' },
        soumettre: async (v) => { cree = await api.installerBac(corpsBac(v)); },
      });
      if (ok) {
        afficherMessage(`Bac ${cree.code} installé dans la zone ${cree.zoneNom} : ses 3 capteurs (niveau, poids, gaz) commencent à émettre.`);
        await controle.rafraichir();
      }
    };

    const modifier = async (b) => {
      const ok = await ouvrirFormulaire({
        titre: `Modifier le bac ${b.code}`, champs: champsBac(false),
        valeurs: { zoneId: b.zoneId, position: { latitude: b.latitude, longitude: b.longitude },
          capaciteLitres: b.capaciteLitres, typeDechet: b.typeDechet },
        soumettre: (v) => api.modifierBac(b.id, corpsBac(v)),
      });
      if (ok) { afficherMessage(`Bac ${b.code} modifié.`); await controle.rafraichir(); }
    };

    const supprimer = async (b) => {
      const ok = await confirmer({
        titre: `Supprimer le bac ${b.code} ?`, danger: true, libelleBouton: 'Supprimer',
        texte: 'Le bac et ses capteurs seront supprimés définitivement. Un bac déjà collecté ne peut pas être supprimé : mettez-le hors service à la place.',
      });
      if (ok) await executer(() => api.supprimerBac(b.id), `Bac ${b.code} supprimé.`, { apres: () => controle.rafraichir() });
    };

    const ligne = (b) => {
      const apres = () => controle.rafraichir();
      const actions = [];
      if (b.etat === 'HORS_SERVICE') {
        actions.push(boutonAction('Remettre en service', () => api.bacRemisEnService(b.id), `Bac ${b.code} remis en service.`, { apres }));
      } else {
        actions.push(boutonAction('Hors service', () => api.bacHorsService(b.id), `Bac ${b.code} mis hors service.`, { apres }));
      }
      if (admin) {
        actions.push(h('button', { class: 'bouton', type: 'button', onclick: () => modifier(b) }, 'Modifier'));
        actions.push(h('button', { class: 'bouton danger', type: 'button', onclick: () => supprimer(b) }, 'Supprimer'));
      }
      return h('tr', {},
        h('td', {}, h('strong', {}, b.code)),
        h('td', {}, b.zoneNom),
        h('td', {}, barreNiveau(b.niveauPourcent, COULEURS[b.etat]), ` ${entier(b.niveauPourcent)} %`),
        h('td', {}, badge(b.etat, LIBELLES_ETAT[b.etat] ?? b.etat)),
        h('td', {}, LIBELLES_DECHET[b.typeDechet] ?? b.typeDechet),
        h('td', {}, `${decimal(b.capaciteLitres)} L`),
        h('td', {}, dateHeure(b.dateDerniereCollecte)),
        h('td', { class: 'actions-ligne' }, actions));
    };

    const afficher = () => {
      const texte = recherche.value.trim().toLowerCase();
      const filtres = bacs.filter((b) => (!choixZone.value || String(b.zoneId) === choixZone.value)
        && (!choixEtat.value || b.etat === choixEtat.value) && (!texte || b.code.toLowerCase().includes(texte)));
      compte.textContent = `${filtres.length} bac(s) affiché(s) sur ${bacs.length}`;
      remplacer(corps, filtres.length === 0
        ? h('tr', {}, h('td', { colspan: 8, class: 'vide' }, 'Aucun bac ne correspond à ces critères.'))
        : filtres.map(ligne));
    };
    for (const e of [choixZone, choixEtat]) e.addEventListener('change', afficher);
    recherche.addEventListener('input', afficher);

    insererDans(conteneur, h('section', { class: 'bloc' },
      h('div', { class: 'barre-outils' }, choixZone, choixEtat, recherche, compte,
        admin ? h('button', { class: 'bouton principal pousse', type: 'button', onclick: installer }, '+ Installer un bac') : null),
      h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
        h('thead', {}, h('tr', {}, ['Code', 'Zone', 'Niveau', 'État', 'Type', 'Capacité', 'Dernière collecte', 'Actions']
          .map((t) => h('th', {}, t)))), corps))));

    controle = boucle(async () => {
      try {
        bacs = await api.bacs();
        afficher();
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    }, 10000);
    courant = controle;
  },

  demonter() { courant?.arreter(); courant = null; },
};
