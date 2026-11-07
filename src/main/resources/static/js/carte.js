import { h, entier } from './dom.js';

export const COULEURS = {
  VIDE: '#2e9e5b',
  MOYEN: '#d9a400',
  PLEIN: '#ef7d1a',
  CRITIQUE: '#d62f2f',
  HORS_SERVICE: '#8a8f98',
};

export const LIBELLES_ETAT = {
  VIDE: 'Vide',
  MOYEN: 'Moyen',
  PLEIN: 'Plein',
  CRITIQUE: 'Critique',
  HORS_SERVICE: 'Hors service',
};

const LIBELLES_CAMION = { DISPONIBLE: 'Disponible', EN_TOURNEE: 'En tournée', EN_PANNE: 'En panne' };

export class Carte {
  constructor(idElement, depot) {
    this.depot = [depot.depotLatitude, depot.depotLongitude];
    this.carte = L.map(idElement).setView(this.depot, 14);
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; contributeurs <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
    }).addTo(this.carte);

    L.marker(this.depot, {
      icon: L.divIcon({ className: 'depot', html: '🏭', iconSize: [24, 24] }),
      keyboard: false,
    }).addTo(this.carte).bindTooltip('Dépôt des camions');

    this.bacs = new Map();
    this.camions = new Map();
    this.groupeTrajet = L.layerGroup().addTo(this.carte);
    this.vueAjustee = false;
  }

  majBacs(bacs) {
    const presents = new Set();
    for (const bac of bacs) {
      presents.add(bac.id);
      const style = { fillColor: COULEURS[bac.etat] ?? COULEURS.HORS_SERVICE };
      let marqueur = this.bacs.get(bac.id);
      if (!marqueur) {
        marqueur = L.circleMarker([bac.latitude, bac.longitude], {
          radius: 9, color: '#ffffff', weight: 2, fillOpacity: 0.95, ...style,
        }).addTo(this.carte);
        marqueur.bindTooltip(bac.code);
        marqueur.bindPopup(() => this.contenuBac(marqueur.donnees));
        this.bacs.set(bac.id, marqueur);
      }
      marqueur.donnees = bac;
      marqueur.setStyle(style);
      if (marqueur.isPopupOpen()) marqueur.getPopup().setContent(this.contenuBac(bac));
    }
    for (const [id, marqueur] of this.bacs) {
      if (!presents.has(id)) {
        marqueur.remove();
        this.bacs.delete(id);
      }
    }
    if (!this.vueAjustee && bacs.length > 0) {
      this.carte.fitBounds(L.latLngBounds(bacs.map((b) => [b.latitude, b.longitude])), { padding: [40, 40] });
      this.vueAjustee = true;
    }
  }

  contenuBac(bac) {
    const couleur = COULEURS[bac.etat] ?? COULEURS.HORS_SERVICE;
    return h('div', {},
      h('strong', {}, bac.code),
      h('div', {}, bac.zoneNom),
      h('div', { class: 'popup-barre' },
        h('span', { style: `width:${Math.min(100, bac.niveauPourcent)}%;background:${couleur}` })),
      h('div', {}, `${entier(bac.niveauPourcent)} % · environ ${entier(bac.poidsEstimeKg)} kg`),
      h('div', {}, `État : ${LIBELLES_ETAT[bac.etat] ?? bac.etat}`));
  }

  majCamions(camions) {
    const presents = new Set();
    for (const camion of camions) {
      if (camion.latitude == null || camion.longitude == null) continue;
      presents.add(camion.id);
      let marqueur = this.camions.get(camion.id);
      if (!marqueur) {
        marqueur = L.marker([camion.latitude, camion.longitude], {
          icon: L.divIcon({ className: 'camion', html: '🚚', iconSize: [28, 28] }),
          keyboard: false,
          zIndexOffset: 1000,
        }).addTo(this.carte);
        this.camions.set(camion.id, marqueur);
      }
      marqueur.setLatLng([camion.latitude, camion.longitude]);
      marqueur.unbindTooltip();
      marqueur.bindTooltip(
        `${camion.immatriculation} · ${LIBELLES_CAMION[camion.statut] ?? camion.statut} · ` +
        `${entier(camion.chargeActuelleKg)} / ${entier(camion.capaciteKg)} kg`);
    }
    for (const [id, marqueur] of this.camions) {
      if (!presents.has(id)) {
        marqueur.remove();
        this.camions.delete(id);
      }
    }
  }

  /** Trace dépôt -> bacs dans l'ordre de passage -> dépôt, avec le numéro d'ordre sur chaque bac. */
  majTrajet(tournee) {
    this.groupeTrajet.clearLayers();
    if (!tournee) return;
    const collectes = [...tournee.collectes].sort((a, b) => a.ordre - b.ordre);
    const points = [this.depot, ...collectes.map((c) => [c.latitude, c.longitude]), this.depot];
    L.polyline(points, {
      color: '#1f6feb', weight: 4, opacity: 0.8,
      dashArray: tournee.statut === 'PLANIFIEE' ? '8 8' : null,
    }).addTo(this.groupeTrajet);
    for (const c of collectes) {
      L.marker([c.latitude, c.longitude], {
        icon: L.divIcon({
          className: `ordre${c.effectuee ? ' faite' : ''}`, html: String(c.ordre), iconSize: [22, 22],
        }),
        interactive: false,
        keyboard: false,
        zIndexOffset: 500,
      }).addTo(this.groupeTrajet);
    }
  }
}
