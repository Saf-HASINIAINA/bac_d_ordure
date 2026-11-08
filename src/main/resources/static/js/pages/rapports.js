import { api } from '../api.js';
import { h, remplacer, entier, decimal, insererDans } from '../dom.js';
import { afficherMessage } from '../ui.js';
import { LIBELLES_ALERTE } from '../libelles.js';

let graphique = null;

/** Date locale au format AAAA-MM-JJ (toISOString donnerait la date UTC, parfois celle de la veille). */
function dateLocale(date) {
  const deux = (n) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${deux(date.getMonth() + 1)}-${deux(date.getDate())}`;
}

const tuile = (valeur, nom) => h('div', { class: 'kpi' }, h('span', { class: 'kpi-valeur' }, valeur), h('span', { class: 'kpi-nom' }, nom));

export default {
  async monter(conteneur) {
    const fin = new Date();
    const debut = new Date();
    debut.setDate(fin.getDate() - 29);

    const champDebut = h('input', { type: 'date', required: true, value: dateLocale(debut), 'aria-label': 'Début' });
    const champFin = h('input', { type: 'date', required: true, value: dateLocale(fin), 'aria-label': 'Fin' });
    const exporter = h('a', { class: 'bouton', href: '#', download: true }, 'Exporter en CSV');
    const kpis = h('div', { class: 'kpis kpis-rapport' });
    const canvas = h('canvas', { 'aria-label': 'Tonnage collecté par jour' });
    const zones = h('tbody');
    const alertes = h('p', { class: 'aide' });
    const formulaire = h('form', { class: 'formulaire' },
      h('label', {}, 'Du', champDebut), h('label', {}, 'Au', champFin),
      h('button', { type: 'submit', class: 'bouton principal' }, 'Afficher'), exporter);

    insererDans(conteneur, h('section', { class: 'bloc' }, formulaire, kpis,
      h('div', { class: 'rapport-grille' },
        h('div', { class: 'graphique-large' }, canvas),
        h('div', { class: 'defilement' }, h('table', { class: 'tableau' },
          h('thead', {}, h('tr', {}, ['Zone', 'Collecté', 'Vidanges', 'Tournées', 'Km'].map((t) => h('th', {}, t)))), zones))),
      alertes));

    const afficher = (r) => {
      remplacer(kpis,
        tuile(`${entier(r.tonnageTotalKg)} kg`, 'Collecté'), tuile(entier(r.nombreCollectes), 'Vidanges'),
        tuile(entier(r.nombreTournees), 'Tournées terminées'), tuile(`${decimal(r.kilometresParcourus)} km`, 'Parcourus'),
        tuile(`${entier(r.tauxRemplissageMoyenALaCollecte)} %`, 'Remplissage moyen à la collecte'));

      const etiquettes = r.parJour.map((j) => `${j.jour.slice(8)}/${j.jour.slice(5, 7)}`);
      const valeurs = r.parJour.map((j) => Math.round(j.tonnageKg * 10) / 10);
      if (!graphique) {
        Chart.defaults.color = getComputedStyle(document.body).color;
        graphique = new Chart(canvas, {
          type: 'bar',
          data: { labels: etiquettes, datasets: [{ label: 'kg collectés', data: valeurs, backgroundColor: '#1f6feb', borderRadius: 3 }] },
          options: { maintainAspectRatio: false, plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true, title: { display: true, text: 'kg par jour' } } } },
        });
      } else {
        graphique.data.labels = etiquettes;
        graphique.data.datasets[0].data = valeurs;
        graphique.update();
      }

      remplacer(zones, r.parZone.length === 0
        ? h('tr', {}, h('td', { colspan: 5, class: 'vide' }, 'Aucune collecte sur cette période.'))
        : r.parZone.map((z) => h('tr', {}, h('td', {}, z.zone), h('td', {}, `${entier(z.tonnageKg)} kg`),
          h('td', {}, entier(z.collectes)), h('td', {}, entier(z.tournees)), h('td', {}, decimal(z.kilometres)))));

      const declenchees = Object.entries(r.alertesParType).filter(([, n]) => n > 0)
        .map(([type, n]) => `${n} ${(LIBELLES_ALERTE[type] ?? type).toLowerCase()}`);
      alertes.textContent = declenchees.length
        ? `Alertes déclenchées sur la période : ${declenchees.join(' · ')}.`
        : 'Aucune alerte déclenchée sur la période.';
    };

    const charger = async () => {
      exporter.href = api.urlExportCsv(champDebut.value, champFin.value);
      try {
        afficher(await api.rapport(champDebut.value, champFin.value));
      } catch (e) {
        if (e.statut !== 401) afficherMessage(e.message, 'erreur');
      }
    };
    formulaire.addEventListener('submit', (e) => { e.preventDefault(); charger(); });
    for (const c of [champDebut, champFin]) c.addEventListener('change', () => { exporter.href = api.urlExportCsv(champDebut.value, champFin.value); });
    await charger();
  },

  demonter() {
    graphique?.destroy();
    graphique = null;
  },
};
