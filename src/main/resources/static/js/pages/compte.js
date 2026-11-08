import { api } from '../api.js';
import { h, insererDans } from '../dom.js';
import { etat } from '../etat.js';
import { executer, afficherMessage } from '../ui.js';
import { LIBELLES_ROLE } from '../libelles.js';

export default {
  async monter(conteneur) {
    const { prenom, nom, email, role } = etat.moi;
    const champ = (libelle, nom2, auto, minlength) => h('label', {}, libelle,
      h('input', { type: 'password', name: nom2, required: true, autocomplete: auto, minlength, maxlength: 72 }));
    const formulaire = h('form', { class: 'formulaire formulaire-colonne' },
      champ('Mot de passe actuel', 'ancien', 'current-password'),
      champ('Nouveau mot de passe (8 caractères minimum)', 'nouveau', 'new-password', 8),
      champ('Confirmer le nouveau mot de passe', 'confirmation', 'new-password', 8),
      h('button', { type: 'submit', class: 'bouton principal' }, 'Changer mon mot de passe'));

    formulaire.addEventListener('submit', async (evenement) => {
      evenement.preventDefault();
      const { ancien, nouveau, confirmation } = formulaire.elements;
      if (nouveau.value !== confirmation.value) {
        afficherMessage('Les deux nouveaux mots de passe ne correspondent pas.', 'erreur');
        return;
      }
      const ok = await executer(() => api.changerMotDePasse({ ancienMotDePasse: ancien.value, nouveauMotDePasse: nouveau.value }),
        'Mot de passe modifié.', { boutons: [formulaire.querySelector('button')] });
      if (ok) formulaire.reset();
    });

    insererDans(conteneur, 
      h('section', { class: 'bloc' }, h('h2', {}, 'Mes informations'),
        h('dl', { class: 'infos' },
          h('dt', {}, 'Nom'), h('dd', {}, `${prenom} ${nom}`),
          h('dt', {}, 'Adresse e-mail'), h('dd', {}, email),
          h('dt', {}, 'Rôle'), h('dd', {}, LIBELLES_ROLE[role] ?? role))),
      h('section', { class: 'bloc' }, h('h2', {}, 'Changer mon mot de passe'), formulaire));
  },
  demonter() {},
};
