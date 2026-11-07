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
  return reponse.json();
}

const post = (url) => requete(url, { method: 'POST' });

export const api = {
  moi: () => requete('/api/auth/moi'),
  deconnexion: () => post('/api/auth/deconnexion'),
  utilisateurs: () => requete('/api/utilisateurs'),
  creerUtilisateur: (corps) => requete('/api/utilisateurs', { method: 'POST', corps }),
  activer: (id) => post(`/api/utilisateurs/${id}/activer`),
  desactiver: (id) => post(`/api/utilisateurs/${id}/desactiver`),
  configuration: () => requete('/api/configuration'),
  statistiques: () => requete('/api/statistiques'),
  zones: () => requete('/api/zones'),
  chauffeurs: () => requete('/api/chauffeurs'),
  camions: () => requete('/api/camions'),
  bacs: () => requete('/api/bacs'),
  alertes: (traitee) => requete(`/api/alertes${traitee === undefined ? '' : `?traitee=${traitee}`}`),
  tournees: () => requete('/api/tournees'),
  planifier: (corps) => requete('/api/tournees', { method: 'POST', corps }),
  demarrer: (id) => post(`/api/tournees/${id}/demarrer`),
  confirmerCollecte: (id, bacId) => post(`/api/tournees/${id}/collectes/${bacId}/confirmer`),
  terminer: (id) => post(`/api/tournees/${id}/terminer`),
  annuler: (id) => post(`/api/tournees/${id}/annuler`),
  traiterAlerte: (id) => post(`/api/alertes/${id}/traiter`),
};
