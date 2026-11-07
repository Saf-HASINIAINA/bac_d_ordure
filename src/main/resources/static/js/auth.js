/** Fonctions communes aux pages de connexion et d'inscription (qui ne passent pas par api.js : pas de redirection sur 401). */

/** Jeton CSRF déposé par le serveur dans un cookie ; à renvoyer dans l'en-tête X-XSRF-TOKEN. */
export function jetonCsrf() {
  const m = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]*)/);
  return m ? decodeURIComponent(m[1]) : '';
}

async function envoyer(url, options) {
  options.method = 'POST';
  options.headers = { Accept: 'application/json', 'X-XSRF-TOKEN': jetonCsrf(), ...(options.headers || {}) };
  let reponse;
  try {
    reponse = await fetch(url, options);
  } catch {
    return { ok: false, statut: 0, donnees: { message: 'Serveur injoignable. Réessayez dans un instant.' } };
  }
  let donnees = {};
  try { donnees = await reponse.json(); } catch { /* corps vide ou non JSON */ }
  return { ok: reponse.ok, statut: reponse.status, donnees };
}

export const postFormulaire = (url, champs) => envoyer(url, {
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  body: new URLSearchParams(champs).toString(),
});

export const postJson = (url, corps) => envoyer(url, {
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(corps),
});

export function afficherMessage(texte, type = 'erreur') {
  const zone = document.getElementById('message');
  zone.textContent = texte;
  zone.className = `message ${type}`;
}
