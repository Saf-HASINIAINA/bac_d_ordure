import { api } from './api.js';
import { h, remplacer, entier } from './dom.js';

const $ = (id) => document.getElementById(id);
const decimal = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 });
const LIBELLES_ALERTE = {
  BAC_PLEIN: 'bacs pleins',
  RISQUE_INCENDIE: "risques d'incendie",
  CAPTEUR_PANNE: 'capteurs en panne',
  INCIDENT_CHAUFFEUR: 'incidents chauffeur',
};

let graphique = null;

/** Date locale au format AAAA-MM-JJ (toISOString donnerait la date UTC, parfois celle de la veille). */
function dateLocale(date) {
  const deux = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${deux(date.getMonth() + 1)}-${deux(date.getDate())}`;
}

export function initialiserRapport(afficherMessage) {
  const fin = new Date();
  const debut = new Date();
  debut.setDate(fin.getDate() - 29);
  $('rapport-debut').value = dateLocale(debut);
  $('rapport-fin').value = dateLocale(fin);
  $('form-rapport').addEventListener('submit', (evenement) => {
    evenement.preventDefault();
    charger(afficherMessage);
  });
  for (const id of ['rapport-debut', 'rapport-fin']) {
    $(id).addEventListener('change', () => { $('rapport-export').href = lienExport(); });
  }
  charger(afficherMessage);
}

const lienExport = () => api.urlExportCsv($('rapport-debut').value, $('rapport-fin').value);

async function charger(afficherMessage) {
  $('rapport-export').href = lienExport();
  try {
    afficher(await api.rapport($('rapport-debut').value, $('rapport-fin').value));
  } catch (e) {
    if (e.statut !== 401) afficherMessage(e.message, 'erreur');
  }
}

function tuile(valeur, nom) {
  return h('div', { class: 'kpi' }, h('span', { class: 'kpi-valeur' }, valeur), h('span', { class: 'kpi-nom' }, nom));
}

function afficher(r) {
  remplacer($('rapport-kpis'),
    tuile(`${entier(r.tonnageTotalKg)} kg`, 'Collecté'),
    tuile(entier(r.nombreCollectes), 'Vidanges'),
    tuile(entier(r.nombreTournees), 'Tournées terminées'),
    tuile(`${decimal.format(r.kilometresParcourus)} km`, 'Parcourus'),
    tuile(`${entier(r.tauxRemplissageMoyenALaCollecte)} %`, 'Remplissage moyen à la collecte'));

  const etiquettes = r.parJour.map((j) => j.jour.slice(8) + '/' + j.jour.slice(5, 7));
  const valeurs = r.parJour.map((j) => Math.round(j.tonnageKg * 10) / 10);
  if (!graphique) {
    graphique = new Chart($('rapport-graphique'), {
      type: 'bar',
      data: { labels: etiquettes, datasets: [{ label: 'kg collectés', data: valeurs, backgroundColor: '#1f6feb', borderRadius: 3 }] },
      options: {
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, title: { display: true, text: 'kg par jour' } } },
      },
    });
  } else {
    graphique.data.labels = etiquettes;
    graphique.data.datasets[0].data = valeurs;
    graphique.update();
  }

  remplacer($('rapport-zones'), r.parZone.length === 0
    ? h('tr', {}, h('td', { colspan: 5, class: 'vide' }, 'Aucune collecte sur cette période.'))
    : r.parZone.map((z) => h('tr', {},
      h('td', {}, z.zone),
      h('td', {}, `${entier(z.tonnageKg)} kg`),
      h('td', {}, entier(z.collectes)),
      h('td', {}, entier(z.tournees)),
      h('td', {}, decimal.format(z.kilometres)))));

  const alertes = Object.entries(r.alertesParType).filter(([, n]) => n > 0)
    .map(([type, n]) => `${n} ${LIBELLES_ALERTE[type] ?? type}`);
  $('rapport-alertes').textContent = alertes.length
    ? `Alertes déclenchées sur la période : ${alertes.join(' · ')}.`
    : 'Aucune alerte déclenchée sur la période.';
}
