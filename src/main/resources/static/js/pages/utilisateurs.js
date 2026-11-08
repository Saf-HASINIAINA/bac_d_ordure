import { api } from '../api.js';
import { h, remplacer, insererDans } from '../dom.js';
import { etat } from '../etat.js';
import { boucle, detecteurChangement, marquerConnexion, boutonAction, badge, afficherMessage } from '../ui.js';
import { LIBELLES_ROLE } from '../libelles.js';
import { ouvrirFormulaire } from '../formulaire.js';

let courant = null;

export default {
  async monter(conteneur) {
    const changement = detecteurChangement();
    let controle = null;
    const corps = h('tbody');
    const compte = h('span', { class: 'compteur zero', title: 'Comptes inactifs (en attente de validation ou désactivés)' }, '0');

    const creer = async () => {
      const ok = await ouvrirFormulaire({
        titre: 'Créer un compte', libelleBouton: 'Créer le compte',
        champs: [
          { nom: 'role', libelle: 'Rôle', type: 'choix', requis: true,
            options: Object.entries(LIBELLES_ROLE).map(([valeur, libelle]) => ({ valeur, libelle })) },
          { nom: 'prenom', libelle: 'Prénom', type: 'texte', requis: true, maxlength: 100 },
          { nom: 'nom', libelle: 'Nom', type: 'texte', requis: true, maxlength: 100 },
          { nom: 'email', libelle: 'Adresse e-mail', type: 'email', requis: true, maxlength: 200 },
          { nom: 'numeroPermis', libelle: 'Numéro de permis', type: 'texte', requis: true, maxlength: 50,
            visibleSi: (v) => v.role === 'CHAUFFEUR' },
          { nom: 'motDePasse', libelle: 'Mot de passe (8 caractères minimum)', type: 'motdepasse', requis: true, minlength: 8, maxlength: 72 },
        ],
        valeurs: { role: 'CHAUFFEUR' },
        soumettre: (v) => api.creerUtilisateur({
          role: v.role, nom: v.nom, prenom: v.prenom, email: v.email, motDePasse: v.motDePasse, numeroPermis: v.numeroPermis || null,
        }),
      });
      if (ok) { afficherMessage('Compte créé : il est actif immédiatement.'); await controle.rafraichir(); }
    };

    const ligne = (u) => {
      const apres = () => controle.rafraichir();
      const nom = `${u.prenom} ${u.nom}`;
      let action = null;
      if (!u.actif) action = boutonAction('Activer', () => api.activer(u.id), `Compte de ${nom} activé.`, { classe: 'principal', apres });
      else if (u.id !== etat.moi.id) action = boutonAction('Désactiver', () => api.desactiver(u.id), `Compte de ${nom} désactivé.`, { classe: 'danger', apres });
      return h('tr', {},
        h('td', {}, h('strong', {}, nom), u.id === etat.moi.id ? h('span', { class: 'aide' }, ' (vous)') : null),
        h('td', {}, u.email), h('td', {}, LIBELLES_ROLE[u.role] ?? u.role), h('td', {}, u.numeroPermis ?? '–'),
        h('td', {}, badge(u.actif ? 'TERMINEE' : 'PLANIFIEE', u.actif ? 'Actif' : 'Inactif')),
        h('td', { class: 'actions-ligne' }, action));
    };

    insererDans(conteneur, h('section', { class: 'bloc' },
      h('div', { class: 'barre-outils' }, h('strong', {}, 'Comptes inactifs : '), compte,
        h('span', { class: 'aide' }, "Un chauffeur qui s'inscrit reste inactif jusqu'à ce que vous l'activiez."),
        h('button', { class: 'bouton principal pousse', type: 'button', onclick: creer }, '+ Créer un compte')),
      h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
        h('thead', {}, h('tr', {}, ['Nom', 'E-mail', 'Rôle', 'Permis', 'Statut', 'Action'].map((t) => h('th', {}, t)))), corps))));

    controle = boucle(async () => {
      try {
        const liste = await api.utilisateurs();
        const inactifs = liste.filter((u) => !u.actif).length;
        compte.textContent = inactifs;
        compte.className = `compteur${inactifs === 0 ? ' zero' : ''}`;
        if (changement('liste', liste)) remplacer(corps, liste.map(ligne));
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    }, 10000);
    courant = controle;
  },
  demonter() { courant?.arreter(); courant = null; },
};
