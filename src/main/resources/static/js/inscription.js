import { postJson, afficherMessage } from './auth.js';

const formulaire = document.getElementById('form-inscription');

formulaire.addEventListener('submit', async (evenement) => {
  evenement.preventDefault();
  const corps = {
    nom: formulaire.nom.value.trim(),
    prenom: formulaire.prenom.value.trim(),
    email: formulaire.email.value.trim(),
    numeroPermis: formulaire.numeroPermis.value.trim(),
    motDePasse: formulaire.motDePasse.value,
  };
  if (Object.values(corps).some((v) => !v)) {
    afficherMessage('Tous les champs sont obligatoires.');
    return;
  }
  if (corps.motDePasse.length < 8) {
    afficherMessage('Le mot de passe doit contenir au moins 8 caractères.');
    return;
  }
  if (corps.motDePasse !== formulaire.confirmation.value) {
    afficherMessage('Les deux mots de passe ne correspondent pas.');
    return;
  }
  const bouton = formulaire.querySelector('button[type="submit"]');
  bouton.disabled = true;
  afficherMessage('');
  const { ok, donnees } = await postJson('/api/auth/inscription', corps);
  if (ok) {
    formulaire.reset();
    for (const element of formulaire.querySelectorAll('label, .deux-colonnes, button, .aide')) element.hidden = true;
    afficherMessage(donnees.message || 'Compte créé.', 'succes');
    return;
  }
  bouton.disabled = false;
  afficherMessage(donnees.message || "Inscription impossible.");
});
