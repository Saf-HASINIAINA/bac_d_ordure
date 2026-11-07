import { postFormulaire, afficherMessage } from './auth.js';

const formulaire = document.getElementById('form-connexion');

// déjà connecté : inutile de ressaisir ses identifiants
fetch('/api/auth/moi', { headers: { Accept: 'application/json' } })
  .then((r) => { if (r.ok) location.replace('/'); })
  .catch(() => { /* serveur injoignable : on affiche simplement le formulaire */ });

formulaire.addEventListener('submit', async (evenement) => {
  evenement.preventDefault();
  const email = formulaire.email.value.trim();
  const motDePasse = formulaire.motDePasse.value;
  if (!email || !motDePasse) {
    afficherMessage('Saisissez votre e-mail et votre mot de passe.');
    return;
  }
  const bouton = formulaire.querySelector('button[type="submit"]');
  bouton.disabled = true;
  afficherMessage('');
  const { ok, donnees } = await postFormulaire('/api/auth/connexion', { email, motDePasse });
  if (ok) {
    location.replace('/');
    return;
  }
  bouton.disabled = false;
  formulaire.motDePasse.value = '';
  formulaire.motDePasse.focus();
  afficherMessage(donnees.message || 'Connexion impossible.');
});
