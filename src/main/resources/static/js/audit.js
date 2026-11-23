import { api } from './api.js';
import { h, remplacer } from './dom.js';

const $ = (id) => document.getElementById(id);
const TAILLE_PAGE = 25;
const formatDate = new Intl.DateTimeFormat('fr-FR', {
  dateStyle: 'short', timeStyle: 'medium', timeZone: 'Indian/Antananarivo',
});

let entrees = [];
let page = 0;

function ligne(e) {
  const echec = e.statut >= 400;
  return h('tr', {},
    h('td', {}, formatDate.format(new Date(e.date))),
    h('td', {}, e.email),
    h('td', {}, e.action),
    h('td', {}, h('span', { class: `badge ${echec ? 'ANNULEE' : 'TERMINEE'}` }, echec ? 'Échec' : 'Réussi')),
    h('td', {}, e.adresseIp ?? ''));
}

function afficher(total, derniere) {
  remplacer($('audit'), entrees.length ? entrees.map(ligne)
    : h('tr', {}, h('td', { class: 'vide', colspan: '5' }, "Aucune action enregistrée.")));
  $('audit-total').textContent = `${entrees.length} sur ${total} actions affichées`;
  $('audit-plus').hidden = derniere;
}

async function charger(depart, afficherMessage) {
  const bouton = depart === 0 ? $('audit-actualiser') : $('audit-plus');
  bouton.disabled = true;
  try {
    const resultat = await api.audit(depart, TAILLE_PAGE);
    entrees = depart === 0 ? resultat.entrees : entrees.concat(resultat.entrees);
    page = resultat.page;
    afficher(resultat.total, resultat.derniere);
  } catch (e) {
    afficherMessage(e.message, 'erreur');
  } finally {
    bouton.disabled = false;
  }
}

/** Journal d'audit (administrateur) : chargé au démarrage puis à la demande, pas d'actualisation automatique. */
export function initialiserAudit(afficherMessage) {
  $('audit-actualiser').addEventListener('click', () => charger(0, afficherMessage));
  $('audit-plus').addEventListener('click', () => charger(page + 1, afficherMessage));
  return charger(0, afficherMessage);
}
