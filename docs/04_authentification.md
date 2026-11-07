# Authentification et droits

## Principe
- **Connexion par session** : `POST /api/auth/connexion` (formulaire `email` + `motDePasse`) dépose un cookie `JSESSIONID`
  (HttpOnly, SameSite=Lax, durée 8 h). Déconnexion : `POST /api/auth/deconnexion`.
- **Mots de passe hachés en BCrypt** (jamais stockés ni renvoyés en clair, 8 à 72 caractères).
- **Protection CSRF** : le serveur dépose un cookie `XSRF-TOKEN` ; tout appel autre que GET doit renvoyer sa valeur
  dans l'en-tête `X-XSRF-TOKEN` (le tableau de bord le fait automatiquement).
- **En-têtes de sécurité** dont une politique CSP (scripts uniquement locaux, tuiles de carte OpenStreetMap autorisées).
- Seules `login.html` et `inscription.html` sont publiques. Sans connexion, l'API répond 401 et les pages redirigent vers `login.html`.

## Inscription et validation des comptes
L'inscription publique (`POST /api/auth/inscription`, page `inscription.html`) crée toujours un **chauffeur inactif** :
personne ne peut s'attribuer un rôle plus élevé. Un administrateur doit **activer** le compte (tableau de bord,
section « Utilisateurs ») avant la première connexion. Un administrateur peut aussi créer directement un compte
actif avec le rôle de son choix.

## Droits par rôle
| Action | Administrateur | Superviseur | Chauffeur |
|---|:-:|:-:|:-:|
| Voir carte, bacs, alertes, statistiques | oui | oui | oui |
| Voir les tournées | toutes | toutes | **les siennes** |
| Planifier / annuler une tournée | oui | oui | non |
| Démarrer, vidanger, terminer une tournée | oui | oui | **les siennes** |
| Traiter une alerte | oui | oui | non |
| Consulter les rapports et les exporter | oui | oui | non |
| Gérer les comptes (lister, créer, activer, désactiver) | oui | non | non |

Les droits sont appliqués **côté serveur** (`SecurityConfig` et `TourneeFacade`) ; masquer un bouton dans la page n'est qu'un confort.

## Endpoints
| Méthode | URL | Accès |
|---|---|---|
| POST | `/api/auth/connexion` | public (formulaire) |
| POST | `/api/auth/deconnexion` | connecté |
| POST | `/api/auth/inscription` | public |
| GET | `/api/auth/moi` | connecté : identité et rôle |
| GET | `/api/utilisateurs` | administrateur |
| POST | `/api/utilisateurs` | administrateur : `{role, nom, prenom, email, motDePasse, numeroPermis}` (permis obligatoire pour un chauffeur) |
| POST | `/api/utilisateurs/{id}/activer` | administrateur |
| POST | `/api/utilisateurs/{id}/desactiver` | administrateur (pas son propre compte) |

Codes d'erreur : 401 non connecté ou identifiants invalides, 403 droits insuffisants ou jeton CSRF absent,
409 email déjà utilisé, 400 données invalides.

## Comptes de démonstration
Créés avec `--app.init-demo=true` : `admin@tulear.mg`, `superviseur@tulear.mg`, `chauffeur@tulear.mg`
(même mot de passe, affiché une seule fois dans les logs, ou fixé par `DEMO_PASSWORD`).

**Mot de passe de démo oublié** :
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.init-demo=true --app.reset-demo-passwords=true"
```
(ajouter `--app.demo-password=...` pour choisir la valeur, sinon elle est générée et affichée).

## Limites connues
- Pas de limitation du nombre d'essais de connexion (à ajouter avant une mise en ligne).
- Pas de réinitialisation du mot de passe par e-mail ni de changement de mot de passe dans l'interface.
- Derrière HTTPS, activer `server.servlet.session.cookie.secure=true`.
- Un message distinct indique qu'un compte existe mais est inactif ; l'email d'un compte existant est aussi révélé à l'inscription.
