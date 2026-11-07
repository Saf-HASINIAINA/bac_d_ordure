import { api } from './api.js';
import { Carte, COULEURS, LIBELLES_ETAT } from './carte.js';
import { h, remplacer, entier, depuis } from './dom.js';

const INTERVALLE_MS = 5000;
const NB_TOURNEES_AFFICHEES = 6;
const NB_ALERTES_AFFICHEES = 8;

const LIBELLES_ALERTE = {
  BAC_PLEIN: 'Bac plein',
  RISQUE_INCENDIE: "Risque d'incendie",
  CAPTEUR_PANNE: 'Capteur en panne',
  INCIDENT_CHAUFFEUR: 'Incident chauffeur',
};
const LIBELLES_ROLE = { ADMIN: 'Administrateur', SUPERVISEUR: 'Superviseur', CHAUFFEUR: 'Chauffeur' };
const LIBELLES_TOURNEE = { PLANIFIEE: 'Planifiée', EN_COURS: 'En cours', TERMINEE: 'Terminée', ANNULEE: 'Annulée' };

const $ = (id) => document.getElementById(id);

const etat = {
  carte: null,
  graphique: null,
  enCours: false,
  tourneeChoisie: null, // tournée dont le trajet est tracé (null = première tournée active)
  signatures: {}, // évite de reconstruire une zone dont les données n'ont pas changé
  tournees: [],
  moi: null, // utilisateur connecté
};

/** Administrateurs et superviseurs gèrent ; un chauffeur ne fait que dérouler ses propres tournées. */
const peutGerer = () => etat.moi.role === 'ADMIN' || etat.moi.role === 'SUPERVISEUR';
const estAdmin = () => etat.moi.role === 'ADMIN';

/* ---------- utilitaires d'affichage ---------- */

function changement(cle, donnees) {
  const signature = JSON.stringify(donnees);
  if (etat.signatures[cle] === signature) return false;
  etat.signatures[cle] = signature;
  return true;
}

let minuteurMessage;
function afficherMessage(texte, type) {
  const zone = $('message');
  zone.textContent = texte;
  zone.className = `message ${type}`;
  clearTimeout(minuteurMessage);
  if (texte) minuteurMessage = setTimeout(() => afficherMessage('', ''), 7000);
}

function marquerConnexion(enLigne) {
  const bloc = $('connexion');
  bloc.className = `connexion ${enLigne ? 'en-ligne' : 'hors-ligne'}`;
  $('connexion-texte').textContent = enLigne
    ? `En direct · mis à jour à ${new Date().toLocaleTimeString('fr-FR')}`
    : 'Serveur injoignable · nouvelle tentative…';
}

/** Exécute une action de l'API, affiche le résultat puis rafraîchit tout de suite la page. */
async function executer(action, messageSucces, boutons = []) {
  boutons.forEach((b) => { b.disabled = true; });
  try {
    await action();
    afficherMessage(messageSucces, 'succes');
  } catch (e) {
    afficherMessage(e.message, 'erreur');
  } finally {
    boutons.forEach((b) => { b.disabled = false; });
    await rafraichir(true);
  }
}

/* ---------- indicateurs et graphique ---------- */

function afficherStatistiques(s) {
  $('kpi-bacs').textContent = entier(s.totalBacs);
  $('kpi-taux').textContent = `${entier(s.tauxRemplissageMoyen)} %`;
  $('kpi-alertes').textContent = entier(s.alertesOuvertes);
  $('kpi-tournees').textContent = entier(s.tourneesEnCours);
  $('kpi-tonnage').textContent = `${entier(s.tonnageCollecteKg)} kg`;

  const etats = Object.keys(LIBELLES_ETAT);
  const graphique = etat.graphique;
  graphique.data.datasets[0].data = etats.map((e) => s.bacsParEtat[e] ?? 0);
  graphique.update('none');
}

function creerGraphique() {
  const etats = Object.keys(LIBELLES_ETAT);
  Chart.defaults.color = getComputedStyle(document.body).color;
  etat.graphique = new Chart($('graphique'), {
    type: 'doughnut',
    data: {
      labels: etats.map((e) => LIBELLES_ETAT[e]),
      datasets: [{ data: etats.map(() => 0), backgroundColor: etats.map((e) => COULEURS[e]), borderWidth: 0 }],
    },
    options: { maintainAspectRatio: false, cutout: '62%', plugins: { legend: { position: 'bottom' } } },
  });
}

function creerLegende() {
  remplacer($('legende'), Object.entries(LIBELLES_ETAT).map(([cle, libelle]) =>
    h('li', {}, h('span', { class: 'pastille', style: `background:${COULEURS[cle]}` }), libelle)));
}

/* ---------- alertes ---------- */

function afficherAlertes(alertes) {
  const compteur = $('nb-alertes');
  compteur.textContent = alertes.length;
  compteur.className = `compteur${alertes.length === 0 ? ' zero' : ''}`;
  if (!changement('alertes', alertes)) return;

  if (alertes.length === 0) {
    remplacer($('alertes'), h('li', { class: 'vide' }, 'Aucune alerte en cours.'));
    return;
  }
  const visibles = alertes.slice(0, NB_ALERTES_AFFICHEES);
  const lignes = visibles.map((a) => {
    const bouton = h('button', { class: 'bouton', type: 'button' }, 'Traiter');
    bouton.addEventListener('click', () =>
      executer(() => api.traiterAlerte(a.id), 'Alerte traitée.', [bouton]));
    return h('li', { class: `alerte${a.type === 'RISQUE_INCENDIE' ? ' incendie' : ''}` },
      h('div', {},
        h('div', { class: 'alerte-titre' }, `${LIBELLES_ALERTE[a.type] ?? a.type}${a.bacCode ? ` · ${a.bacCode}` : ''}`),
        h('div', { class: 'alerte-detail' }, `${a.message} · ${depuis(a.dateCreation)}`)),
      peutGerer() ? bouton : null);
  });
  if (alertes.length > visibles.length) {
    lignes.push(h('li', { class: 'vide' }, `+ ${alertes.length - visibles.length} autre(s) alerte(s)`));
  }
  remplacer($('alertes'), lignes);
}

/* ---------- formulaire de planification ---------- */

function remplirSelect(select, options, vide) {
  const valeur = select.value;
  const contenu = options.length === 0
    ? [h('option', { value: '' }, vide)]
    : options.map((o) => h('option', { value: o.valeur }, o.libelle));
  remplacer(select, contenu);
  if (options.some((o) => String(o.valeur) === valeur)) select.value = valeur;
}

function afficherCamionsDisponibles(camions) {
  if (!changement('camions-form', camions.map((c) => [c.id, c.statut]))) return;
  const disponibles = camions.filter((c) => c.statut === 'DISPONIBLE');
  remplirSelect($('champ-camion'),
    disponibles.map((c) => ({ valeur: c.id, libelle: `${c.immatriculation} (${entier(c.capaciteKg)} kg)` })),
    'Aucun camion disponible');
}

function brancherFormulaire() {
  $('form-tournee').addEventListener('submit', (evenement) => {
    evenement.preventDefault();
    const corps = {
      zoneId: Number($('champ-zone').value),
      camionId: Number($('champ-camion').value),
      chauffeurId: Number($('champ-chauffeur').value),
    };
    if (!corps.zoneId || !corps.camionId || !corps.chauffeurId) {
      afficherMessage('Choisissez une zone, un camion et un chauffeur.', 'erreur');
      return;
    }
    const bouton = evenement.currentTarget.querySelector('button[type="submit"]');
    executer(async () => {
      const tournee = await api.planifier(corps);
      etat.tourneeChoisie = tournee.id;
    }, 'Tournée planifiée : elle est tracée en pointillés sur la carte.', [bouton]);
  });
}

/* ---------- tournées ---------- */

function tourneeAffichee() {
  const choisie = etat.tournees.find((t) => t.id === etat.tourneeChoisie);
  if (choisie) return choisie;
  return etat.tournees.find((t) => t.statut === 'EN_COURS' || t.statut === 'PLANIFIEE') ?? null;
}

function boutonAction(libelle, action, message, classe = '') {
  const bouton = h('button', { class: `bouton ${classe}`.trim(), type: 'button' }, libelle);
  bouton.addEventListener('click', () => executer(action, message, [bouton]));
  return bouton;
}

function carteTournee(t, selectionnee) {
  const total = t.collectes.length;
  const faites = t.collectes.filter((c) => c.effectuee).length;

  const lignes = t.collectes.map((c) => {
    const contenu = [h('span', {}, `${c.ordre}. ${c.bacCode} · ${entier(c.niveauPourcent)} %`)];
    if (c.effectuee) {
      contenu.push(h('span', {}, `${entier(c.poidsCollecteKg)} kg ✓`));
    } else if (t.statut === 'EN_COURS') {
      contenu.push(boutonAction('Confirmer la vidange', () => api.confirmerCollecte(t.id, c.bacId),
        `Bac ${c.bacCode} vidé.`));
    }
    return h('li', { class: `collecte${c.effectuee ? ' faite' : ''}` }, contenu);
  });

  const actions = [];
  const voir = h('button', { class: 'bouton', type: 'button' }, 'Voir sur la carte');
  voir.addEventListener('click', () => {
    etat.tourneeChoisie = t.id;
    afficherTrajet();
    etat.signatures.tournees = null;
    afficherTournees(etat.tournees);
  });
  actions.push(voir);
  if (t.statut === 'PLANIFIEE') {
    actions.push(boutonAction('Démarrer', () => api.demarrer(t.id), `Tournée n°${t.id} démarrée.`, 'principal'));
    if (peutGerer()) {
      actions.push(boutonAction('Annuler', () => api.annuler(t.id), `Tournée n°${t.id} annulée.`, 'danger'));
    }
  }
  if (t.statut === 'EN_COURS') {
    actions.push(boutonAction('Terminer', () => api.terminer(t.id), `Tournée n°${t.id} terminée.`, 'principal'));
  }

  return h('article', { class: `tournee${selectionnee ? ' selection' : ''}` },
    h('div', { class: 'tournee-tete' },
      h('strong', {}, `Tournée n°${t.id} · ${t.zoneNom}`),
      h('span', { class: `badge ${t.statut}` }, LIBELLES_TOURNEE[t.statut] ?? t.statut)),
    h('div', { class: 'tournee-meta' },
      `${t.camionImmatriculation} · ${t.chauffeurNom} · ${t.distanceKm.toFixed(1)} km · ${t.date}`),
    h('progress', { max: total, value: faites, 'aria-label': 'Bacs vidés' }),
    h('div', { class: 'tournee-meta' },
      `${faites} / ${total} bacs vidés · collecté ${entier(t.poidsCollecteKg)} kg · attendu ${entier(t.poidsAttenduKg)} kg`),
    h('ul', { class: 'collectes' }, lignes),
    h('div', { class: 'actions' }, actions));
}

function afficherTournees(tournees) {
  etat.tournees = tournees;
  const actives = tournees.filter((t) => t.statut === 'EN_COURS' || t.statut === 'PLANIFIEE');
  const autres = tournees.filter((t) => !actives.includes(t));
  const visibles = [...actives, ...autres].slice(0, NB_TOURNEES_AFFICHEES);
  const choix = tourneeAffichee()?.id ?? null;

  if (!changement('tournees', { visibles, choix })) return;
  if (visibles.length === 0) {
    remplacer($('tournees'), h('p', { class: 'vide' },
      peutGerer() ? 'Aucune tournée pour le moment. Planifiez-en une ci-dessus.' : 'Aucune tournée ne vous est confiée pour le moment.'));
    return;
  }
  remplacer($('tournees'), visibles.map((t) => carteTournee(t, t.id === choix)));
}

function afficherTrajet() {
  const tournee = tourneeAffichee();
  if (changement('trajet', tournee)) etat.carte.majTrajet(tournee);
}

/* ---------- utilisateur connecté ---------- */

function afficherUtilisateurConnecte() {
  const { prenom, nom, role } = etat.moi;
  $('utilisateur-nom').textContent = `${prenom} ${nom}`;
  $('utilisateur-role').textContent = LIBELLES_ROLE[role] ?? role;
  $('utilisateur').hidden = false;
  $('bouton-deconnexion').addEventListener('click', async (e) => {
    e.currentTarget.disabled = true;
    try { await api.deconnexion(); } catch { /* même en cas d'échec réseau, on repart sur la page de connexion */ }
    location.replace('/login.html');
  });
  // les droits sont aussi contrôlés côté serveur : masquer ici n'est qu'un confort
  $('bloc-planification').hidden = !peutGerer();
  $('bloc-utilisateurs').hidden = !estAdmin();
  if (!peutGerer()) $('titre-tournees').textContent = 'Mes tournées';
}

/* ---------- gestion des comptes (administrateur) ---------- */

function afficherUtilisateurs(liste) {
  const inactifs = liste.filter((u) => !u.actif).length;
  const compteur = $('nb-attente');
  compteur.textContent = inactifs;
  compteur.className = `compteur${inactifs === 0 ? ' zero' : ''}`;
  if (!changement('utilisateurs', liste)) return;

  remplacer($('utilisateurs'), liste.map((u) => {
    const nomComplet = `${u.prenom} ${u.nom}`;
    let action = null;
    if (!u.actif) {
      action = boutonAction('Activer', () => api.activer(u.id), `Compte de ${nomComplet} activé.`, 'principal');
    } else if (u.id !== etat.moi.id) {
      action = boutonAction('Désactiver', () => api.desactiver(u.id), `Compte de ${nomComplet} désactivé.`, 'danger');
    }
    return h('tr', {},
      h('td', {}, nomComplet),
      h('td', {}, u.email),
      h('td', {}, LIBELLES_ROLE[u.role] ?? u.role),
      h('td', {}, h('span', { class: `badge ${u.actif ? 'TERMINEE' : 'PLANIFIEE'}` }, u.actif ? 'Actif' : 'Inactif')),
      h('td', {}, action));
  }));
}

function brancherFormulaireUtilisateur() {
  const role = $('u-role');
  const basculerPermis = () => { $('u-permis-bloc').hidden = role.value !== 'CHAUFFEUR'; };
  role.addEventListener('change', basculerPermis);
  basculerPermis();

  $('form-utilisateur').addEventListener('submit', (evenement) => {
    evenement.preventDefault();
    const corps = {
      role: role.value,
      nom: $('u-nom').value.trim(),
      prenom: $('u-prenom').value.trim(),
      email: $('u-email').value.trim(),
      motDePasse: $('u-mdp').value,
      numeroPermis: $('u-permis').value.trim() || null,
    };
    executer(async () => {
      await api.creerUtilisateur(corps);
      evenement.target.reset();
      basculerPermis();
    }, `Compte de ${corps.prenom} ${corps.nom} créé.`,
      [evenement.currentTarget.querySelector('button[type="submit"]')]);
  });
}

/* ---------- boucle de rafraîchissement ---------- */

async function rafraichir(force = false) {
  if (etat.enCours || (document.hidden && !force)) return;
  etat.enCours = true;
  try {
    const [stats, bacs, alertes, camions, tournees, utilisateurs] = await Promise.all([
      api.statistiques(), api.bacs(), api.alertes(false), api.camions(), api.tournees(),
      estAdmin() ? api.utilisateurs() : Promise.resolve(null),
    ]);
    afficherStatistiques(stats);
    etat.carte.majBacs(bacs);
    etat.carte.majCamions(camions);
    afficherAlertes(alertes);
    if (peutGerer()) afficherCamionsDisponibles(camions);
    if (utilisateurs) afficherUtilisateurs(utilisateurs);
    afficherTournees(tournees);
    afficherTrajet();
    marquerConnexion(true);
  } catch (e) {
    console.error(e);
    marquerConnexion(false);
  } finally {
    etat.enCours = false;
  }
}

async function demarrer() {
  try {
    etat.moi = await api.moi(); // sans session, api.js renvoie vers la page de connexion
  } catch (e) {
    if (e.statut !== 401) {
      marquerConnexion(false);
      afficherMessage("Impossible de joindre le serveur. Vérifiez que l'application est démarrée.", 'erreur');
    }
    return;
  }
  afficherUtilisateurConnecte();
  creerLegende();
  creerGraphique();
  brancherFormulaire();
  if (estAdmin()) brancherFormulaireUtilisateur();
  try {
    const [configuration, zones, chauffeurs] = await Promise.all([
      api.configuration(),
      peutGerer() ? api.zones() : Promise.resolve([]),
      peutGerer() ? api.chauffeurs() : Promise.resolve([]),
    ]);
    etat.carte = new Carte('carte', configuration);
    remplirSelect($('champ-zone'), zones.map((z) => ({ valeur: z.id, libelle: z.nom })), 'Aucune zone');
    remplirSelect($('champ-chauffeur'),
      chauffeurs.map((c) => ({ valeur: c.id, libelle: `${c.prenom} ${c.nom}` })), 'Aucun chauffeur');
  } catch (e) {
    console.error(e);
    marquerConnexion(false);
    afficherMessage("Impossible de joindre le serveur. Vérifiez que l'application est démarrée.", 'erreur');
    return;
  }
  await rafraichir();
  setInterval(rafraichir, INTERVALLE_MS);
  document.addEventListener('visibilitychange', () => { if (!document.hidden) rafraichir(); });
}

demarrer();
