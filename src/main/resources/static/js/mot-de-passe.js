import { postJson, afficherMessage } from './auth.js';

const formulaire = document.getElementById('form-mot-de-passe');

formulaire.addEventListener('submit', async (evenement) => {
  evenement.preventDefault();
  const ancien = formulaire.ancien.value;
  const nouveau = formulaire.nouveau.value;
  if (!ancien || !nouveau) {
    afficherMessage('Remplissez tous les champs.');
    return;
  }
  if (nouveau.length < 8 || nouveau.length > 72) {
    afficherMessage('Le nouveau mot de passe doit contenir entre 8 et 72 caractères.');
    return;
  }
  if (nouveau !== formulaire.confirmation.value) {
    afficherMessage('La confirmation ne correspond pas au nouveau mot de passe.');
    return;
  }
  const bouton = formulaire.querySelector('button[type="submit"]');
  bouton.disabled = true;
  afficherMessage('');
  const { ok, statut, donnees } = await postJson('/api/auth/mot-de-passe',
    { ancienMotDePasse: ancien, nouveauMotDePasse: nouveau });
  if (statut === 401) { // session expirée
    location.replace('/login.html');
    return;
  }
  bouton.disabled = false;
  if (ok) {
    formulaire.reset();
    afficherMessage('Mot de passe modifié.', 'succes');
    return;
  }
  formulaire.ancien.focus();
  afficherMessage(donnees.message || 'Modification impossible.');
});
