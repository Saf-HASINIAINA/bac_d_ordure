import { h } from './dom.js';
import { etat } from './etat.js';

/** Boîte de confirmation (suppression, etc.). Résout true si l'utilisateur confirme. */
export function confirmer({ titre, texte, libelleBouton = 'Confirmer', danger = false }) {
  return new Promise((resolve) => {
    let confirme = false;
    const annuler = h('button', { type: 'button', class: 'bouton' }, 'Annuler');
    const ok = h('button', { type: 'button', class: `bouton ${danger ? 'danger-plein' : 'principal'}` }, libelleBouton);
    const dialogue = h('dialog', { class: 'dialogue' },
      h('h2', {}, titre), h('p', {}, texte), h('div', { class: 'dialogue-actions' }, annuler, ok));
    annuler.addEventListener('click', () => dialogue.close());
    ok.addEventListener('click', () => { confirme = true; dialogue.close(); });
    dialogue.addEventListener('close', () => { dialogue.remove(); resolve(confirme); });
    document.body.append(dialogue);
    dialogue.showModal();
    annuler.focus(); // le choix par défaut est le plus prudent
  });
}

/* ---------- champs ---------- */

function creerChamp(champ, valeur) {
  if (champ.type === 'position') return creerChampPosition(champ, valeur);

  let entree;
  if (champ.type === 'choix') {
    entree = h('select', { name: champ.nom },
      (champ.options ?? []).map((o) => h('option', { value: o.valeur }, o.libelle)));
    entree.value = valeur == null ? '' : String(valeur);
  } else if (champ.type === 'zonetexte') {
    entree = h('textarea', { name: champ.nom, rows: 4, maxlength: champ.maxlength, minlength: champ.minlength, placeholder: champ.placeholder });
    if (valeur != null) entree.value = valeur;
  } else {
    const type = { texte: 'text', email: 'email', motdepasse: 'password', nombre: 'number' }[champ.type ?? 'texte'];
    entree = h('input', {
      name: champ.nom, type, maxlength: champ.maxlength, minlength: champ.minlength, min: champ.min, max: champ.max,
      step: champ.step, placeholder: champ.placeholder,
      autocomplete: champ.type === 'motdepasse' ? 'new-password' : 'off',
    });
    if (valeur != null) entree.value = valeur;
  }
  entree.required = !!champ.requis;
  const element = h('label', {}, champ.libelle,
    champ.aide ? h('span', { class: 'aide' }, champ.aide) : null, entree);
  return {
    element,
    setRequis: (requis) => { entree.required = requis; },
    lire() {
      if (champ.type === 'nombre') return entree.value === '' ? null : Number(entree.value);
      if (champ.type === 'motdepasse') return entree.value;
      return entree.value.trim();
    },
    demarrer() { /* rien à initialiser */ },
    detruire() { /* rien à libérer */ },
  };
}

/** Choix d'un point en cliquant sur une petite carte, ou en saisissant latitude et longitude. */
function creerChampPosition(champ, valeur) {
  const defaut = etat.configuration
    ? { latitude: etat.configuration.depotLatitude, longitude: etat.configuration.depotLongitude }
    : { latitude: -23.355, longitude: 43.67 };
  const centre = valeur ?? champ.centre ?? defaut;
  const zoneCarte = h('div', { class: 'mini-carte' });
  const latitude = h('input', { type: 'number', step: 'any', min: -90, max: 90, placeholder: 'Latitude', 'aria-label': 'Latitude' });
  const longitude = h('input', { type: 'number', step: 'any', min: -180, max: 180, placeholder: 'Longitude', 'aria-label': 'Longitude' });
  latitude.required = longitude.required = !!champ.requis;
  if (valeur) {
    latitude.value = valeur.latitude;
    longitude.value = valeur.longitude;
  }
  let carte = null;
  let repere = null;

  const placer = () => {
    if (!carte || latitude.value === '' || longitude.value === '') return;
    const point = [Number(latitude.value), Number(longitude.value)];
    if (repere) repere.setLatLng(point);
    else repere = L.circleMarker(point, { radius: 9, color: '#fff', weight: 2, fillColor: '#1f6feb', fillOpacity: 1 }).addTo(carte);
  };

  const element = h('div', { class: 'champ-position' },
    h('span', { class: 'libelle' }, champ.libelle),
    h('span', { class: 'aide' }, champ.aide ?? 'Cliquez sur la carte pour placer le point.'),
    zoneCarte,
    h('div', { class: 'deux-colonnes' }, latitude, longitude));

  return {
    element,
    setRequis: (requis) => { latitude.required = longitude.required = requis; },
    lire() {
      return latitude.value !== '' && longitude.value !== ''
        ? { latitude: Number(latitude.value), longitude: Number(longitude.value) }
        : null;
    },
    demarrer() { // appelé une fois la boîte affichée (Leaflet a besoin d'une taille)
      carte = L.map(zoneCarte).setView([centre.latitude, centre.longitude], valeur ? 16 : 14);
      L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19,
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(carte);
      carte.on('click', (e) => {
        latitude.value = e.latlng.lat.toFixed(6);
        longitude.value = e.latlng.lng.toFixed(6);
        placer();
      });
      latitude.addEventListener('change', placer);
      longitude.addEventListener('change', placer);
      placer();
      setTimeout(() => carte && carte.invalidateSize(), 80);
    },
    detruire() { // la carte peut être détruite avant la mise à jour de taille différée : on la neutralise
      if (carte) carte.remove();
      carte = null;
    },
  };
}

/**
 * Boîte de saisie.
 * @param champs  [{ nom, libelle, type: texte|zonetexte|email|motdepasse|nombre|choix|position, requis, options, min, max, step,
 *                  maxlength, aide, visibleSi(valeurs) }]
 * @param soumettre  async (valeurs) => ... ; si elle lève une erreur, le message s'affiche dans la boîte qui reste ouverte
 * @returns true si la saisie a été enregistrée
 */
export function ouvrirFormulaire({ titre, champs, valeurs = {}, libelleBouton = 'Enregistrer', soumettre }) {
  return new Promise((resolve) => {
    let reussi = false;
    const objets = champs.map((c) => ({ definition: c, champ: creerChamp(c, valeurs[c.nom]) }));
    const erreur = h('p', { class: 'message erreur', role: 'alert' });
    const annuler = h('button', { type: 'button', class: 'bouton' }, 'Annuler');
    const valider = h('button', { type: 'submit', class: 'bouton principal' }, libelleBouton);
    const formulaire = h('form', { class: 'dialogue-formulaire', novalidate: false },
      objets.map((o) => o.champ.element), erreur, h('div', { class: 'dialogue-actions' }, annuler, valider));
    const dialogue = h('dialog', { class: 'dialogue' }, h('h2', {}, titre), formulaire);

    const lireTout = () => {
      const lues = {};
      for (const { definition, champ } of objets) lues[definition.nom] = champ.element.hidden ? null : champ.lire();
      return lues;
    };
    const appliquerVisibilite = () => {
      const lues = lireTout();
      for (const { definition, champ } of objets) {
        if (!definition.visibleSi) continue;
        const visible = !!definition.visibleSi(lues);
        champ.element.hidden = !visible;
        champ.setRequis(visible && !!definition.requis);
      }
    };
    formulaire.addEventListener('input', appliquerVisibilite);
    formulaire.addEventListener('change', appliquerVisibilite);

    formulaire.addEventListener('submit', async (evenement) => {
      evenement.preventDefault();
      erreur.textContent = '';
      valider.disabled = true;
      try {
        await soumettre(lireTout());
        reussi = true;
        dialogue.close();
      } catch (e) {
        erreur.textContent = e.message;
        valider.disabled = false;
      }
    });
    annuler.addEventListener('click', () => dialogue.close());
    dialogue.addEventListener('close', () => {
      objets.forEach((o) => o.champ.detruire());
      dialogue.remove();
      resolve(reussi);
    });

    document.body.append(dialogue);
    dialogue.showModal();
    appliquerVisibilite();
    objets.forEach((o) => o.champ.demarrer());
    const premier = dialogue.querySelector('input:not([hidden]), select');
    if (premier) premier.focus();
  });
}
