export class ErreurApi extends Error {
  constructor(message, statut) {
    super(message);
    this.statut = statut;
  }
}

function jetonCsrf() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return m ? decodeURIComponent(m[1]) : '';
}

async function requete(url, { method = 'GET', corps } = {}) {
  const options = { method, headers: { Accept: 'application/json' } };
  if (method !== 'GET') options.headers['X-XSRF-TOKEN'] = jetonCsrf();
  if (corps !== undefined) {
    options.headers['Content-Type'] = 'application/json';
    options.body = JSON.stringify(corps);
  }
  const reponse = await fetch(url, options);
  if (reponse.status === 401) {
    // session expirée ou absente : retour à la page de connexion
    location.replace('/login.html');
    throw new ErreurApi('Session expirée', 401);
  }
  if (!reponse.ok) {
    let message = `Erreur ${reponse.status}`;
    try {
      message = (await reponse.json()).message || message;
    } catch {
      /* corps non JSON : on garde le message par défaut */
    }
    throw new ErreurApi(message, reponse.status);
  }
  return reponse.status === 204 ? null : reponse.json();
}

const post = (url, corps) => requete(url, { method: 'POST', corps });
const put = (url, corps) => requete(url, { method: 'PUT', corps });
const supprimer = (url) => requete(url, { method: 'DELETE' });
const q = encodeURIComponent;

export const api = {
  // session et compte
  moi: () => requete('/api/auth/moi'),
  deconnexion: () => post('/api/auth/deconnexion'),
  changerMotDePasse: (corps) => post('/api/auth/mot-de-passe', corps),
  configuration: () => requete('/api/configuration'),
  // consultation
  statistiques: () => requete('/api/statistiques'),
  bacs: () => requete('/api/bacs'),
  camions: () => requete('/api/camions'),
  zones: () => requete('/api/zones'),
  chauffeurs: () => requete('/api/chauffeurs'),
  alertes: (traitee) => requete(`/api/alertes${traitee === undefined ? '' : `?traitee=${traitee}`}`),
  tournees: () => requete('/api/tournees'),
  rapport: (debut, fin) => requete(`/api/rapports?debut=${q(debut)}&fin=${q(fin)}`),
  urlExportCsv: (debut, fin) => `/api/rapports/export.csv?debut=${q(debut)}&fin=${q(fin)}`,
  // bacs
  installerBac: (corps) => post('/api/bacs', corps),
  modifierBac: (id, corps) => put(`/api/bacs/${id}`, corps),
  supprimerBac: (id) => supprimer(`/api/bacs/${id}`),
  bacHorsService: (id) => post(`/api/bacs/${id}/hors-service`),
  bacRemisEnService: (id) => post(`/api/bacs/${id}/remise-en-service`),
  // zones
  creerZone: (corps) => post('/api/zones', corps),
  modifierZone: (id, corps) => put(`/api/zones/${id}`, corps),
  supprimerZone: (id) => supprimer(`/api/zones/${id}`),
  // camions
  creerCamion: (corps) => post('/api/camions', corps),
  modifierCamion: (id, corps) => put(`/api/camions/${id}`, corps),
  supprimerCamion: (id) => supprimer(`/api/camions/${id}`),
  camionEnPanne: (id) => post(`/api/camions/${id}/panne`),
  camionDisponible: (id) => post(`/api/camions/${id}/disponible`),
  // tournées et alertes
  planifier: (corps) => post('/api/tournees', corps),
  demarrer: (id) => post(`/api/tournees/${id}/demarrer`),
  confirmerCollecte: (id, bacId) => post(`/api/tournees/${id}/collectes/${bacId}/confirmer`),
  terminer: (id) => post(`/api/tournees/${id}/terminer`),
  annuler: (id) => post(`/api/tournees/${id}/annuler`),
  signalerIncident: (id, corps) => post(`/api/tournees/${id}/incidents`, corps),
  traiterAlerte: (id) => post(`/api/alertes/${id}/traiter`),
  // comptes
  utilisateurs: () => requete('/api/utilisateurs'),
  creerUtilisateur: (corps) => post('/api/utilisateurs', corps),
  activer: (id) => post(`/api/utilisateurs/${id}/activer`),
  desactiver: (id) => post(`/api/utilisateurs/${id}/desactiver`),
};
