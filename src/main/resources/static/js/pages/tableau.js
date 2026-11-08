import { api } from '../api.js';
import { Carte } from '../carte.js';
import { h, remplacer, entier, depuis, insererDans } from '../dom.js';
import { etat, peutGerer } from '../etat.js';
import { boucle, detecteurChangement, marquerConnexion, badge } from '../ui.js';
import { COULEURS, LIBELLES_ETAT, LIBELLES_ALERTE, LIBELLES_TOURNEE } from '../libelles.js';

let courant = null;

const tuile = (id, nom) => h('div', { class: 'kpi' }, h('span', { class: 'kpi-valeur', id }, '–'), h('span', { class: 'kpi-nom' }, nom));
const texte = (id, valeur) => { document.getElementById(id).textContent = valeur; };

/** Tournée dont on trace le trajet : celle choisie, sinon la première en cours ou planifiée. */
function tourneeAffichee(tournees) {
  return tournees.find((t) => t.id === etat.tourneeChoisie)
    ?? tournees.find((t) => t.statut === 'EN_COURS' || t.statut === 'PLANIFIEE') ?? null;
}

function creerGraphique(canvas) {
  const etats = Object.keys(LIBELLES_ETAT);
  Chart.defaults.color = getComputedStyle(document.body).color;
  return new Chart(canvas, {
    type: 'doughnut',
    data: {
      labels: etats.map((e) => LIBELLES_ETAT[e]),
      datasets: [{ data: etats.map(() => 0), backgroundColor: etats.map((e) => COULEURS[e]), borderWidth: 0 }],
    },
    options: { maintainAspectRatio: false, cutout: '62%', plugins: { legend: { position: 'bottom' } } },
  });
}

function afficherAlertes(alertes, liste, compteur) {
  compteur.textContent = alertes.length;
  compteur.className = `compteur${alertes.length === 0 ? ' zero' : ''}`;
  if (alertes.length === 0) {
    remplacer(liste, h('li', { class: 'vide' }, 'Aucune alerte en cours.'));
    return;
  }
  const lignes = alertes.slice(0, 5).map((a) => h('li', { class: `alerte${a.type === 'RISQUE_INCENDIE' ? ' incendie' : ''}` },
    h('div', {},
      h('div', { class: 'alerte-titre' }, `${LIBELLES_ALERTE[a.type] ?? a.type}${a.bacCode ? ` · ${a.bacCode}` : ''}`),
      h('div', { class: 'alerte-detail' }, `${a.message} · ${depuis(a.dateCreation)}`))));
  if (alertes.length > 5) {
    lignes.push(h('li', { class: 'vide' }, `+ ${alertes.length - 5} autre(s) alerte(s)`));
  }
  remplacer(liste, lignes);
}

/** Résumé pour un chauffeur : où il en est et quel est son prochain bac. */
function afficherMaTournee(tournees, conteneur) {
  const t = tournees.find((x) => x.statut === 'EN_COURS') ?? tournees.find((x) => x.statut === 'PLANIFIEE');
  if (!t) {
    remplacer(conteneur, h('p', { class: 'vide' }, "Aucune tournée ne vous est confiée pour le moment."));
    return;
  }
  const faites = t.collectes.filter((c) => c.effectuee).length;
  const suivant = [...t.collectes].sort((a, b) => a.ordre - b.ordre).find((c) => !c.effectuee);
  remplacer(conteneur,
    h('div', { class: 'tournee-tete' }, h('strong', {}, `Tournée n°${t.id} · ${t.zoneNom}`), badge(t.statut, LIBELLES_TOURNEE[t.statut])),
    h('progress', { max: t.collectes.length, value: faites, 'aria-label': 'Bacs vidés' }),
    h('p', { class: 'tournee-meta' }, `${faites} / ${t.collectes.length} bacs vidés`),
    suivant
      ? h('p', {}, 'Prochain bac : ', h('strong', {}, suivant.bacCode), ` (étape ${suivant.ordre} sur ${t.collectes.length})`)
      : h('p', {}, 'Tous les bacs sont vidés : vous pouvez terminer la tournée.'),
    h('a', { class: 'bouton principal', href: '#/tournees' }, 'Ouvrir mes tournées'));
}

export default {
  async monter(conteneur) {
    const gerer = peutGerer();
    const changement = detecteurChangement();

    const listeAlertes = h('ul', { class: 'alertes' });
    const compteur = h('span', { class: 'compteur zero' }, '0');
    const canvas = h('canvas', { 'aria-label': 'Répartition des bacs par état' });
    const maTournee = h('div', { class: 'ma-tournee' });
    const zoneCarte = h('div', { class: 'carte-grande', role: 'img', 'aria-label': 'Carte des bacs et des camions' });

    const colonne = gerer
      ? h('aside', { class: 'colonne' },
        h('section', { class: 'bloc' }, h('h2', {}, 'Répartition des bacs'), h('div', { class: 'graphique' }, canvas)),
        h('section', { class: 'bloc' },
          h('h2', {}, 'Alertes à traiter ', compteur), listeAlertes,
          h('a', { class: 'lien-section', href: '#/alertes' }, 'Voir toutes les alertes →')))
      : h('aside', { class: 'colonne' },
        h('section', { class: 'bloc' }, h('h2', {}, 'Ma tournée'), maTournee));

    insererDans(conteneur, 
      gerer ? h('section', { class: 'kpis', 'aria-label': 'Indicateurs' },
        tuile('kpi-bacs', 'Bacs suivis'), tuile('kpi-taux', 'Remplissage moyen'), tuile('kpi-alertes', 'Alertes ouvertes'),
        tuile('kpi-tournees', 'Tournées en cours'), tuile('kpi-tonnage', 'Collecté')) : null,
      h('div', { class: 'grille' },
        h('section', { class: 'bloc bloc-carte', 'aria-label': 'Carte' }, zoneCarte,
          h('ul', { class: 'legende' }, Object.entries(LIBELLES_ETAT).map(([cle, libelle]) =>
            h('li', {}, h('span', { class: 'pastille', style: `background:${COULEURS[cle]}` }), libelle)))),
        colonne));

    const configuration = etat.configuration ?? await api.configuration();
    const carte = new Carte(zoneCarte, configuration);
    const graphique = gerer ? creerGraphique(canvas) : null;

    const rafraichir = async () => {
      try {
        const [bacs, camions, tournees, stats, alertes] = await Promise.all([
          api.bacs(), api.camions(), api.tournees(),
          gerer ? api.statistiques() : null, gerer ? api.alertes(false) : null,
        ]);
        carte.majBacs(bacs);
        carte.majCamions(camions);
        const affichee = tourneeAffichee(tournees);
        if (changement('trajet', affichee)) carte.majTrajet(affichee);

        if (stats) {
          texte('kpi-bacs', entier(stats.totalBacs));
          texte('kpi-taux', `${entier(stats.tauxRemplissageMoyen)} %`);
          texte('kpi-alertes', entier(stats.alertesOuvertes));
          texte('kpi-tournees', entier(stats.tourneesEnCours));
          texte('kpi-tonnage', `${entier(stats.tonnageCollecteKg)} kg`);
          graphique.data.datasets[0].data = Object.keys(LIBELLES_ETAT).map((e) => stats.bacsParEtat[e] ?? 0);
          graphique.update('none');
        }
        if (alertes) afficherAlertes(alertes, listeAlertes, compteur);
        if (!gerer && changement('ma-tournee', tournees)) afficherMaTournee(tournees, maTournee);
        marquerConnexion(true);
      } catch (e) {
        if (e.statut !== 401) marquerConnexion(false);
      }
    };

    courant = { boucle: boucle(rafraichir, 5000), carte, graphique };
  },

  demonter() {
    if (!courant) return;
    courant.boucle.arreter();
    courant.carte.detruire();
    courant.graphique?.destroy();
    courant = null;
  },
};
