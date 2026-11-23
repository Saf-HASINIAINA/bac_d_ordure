# Authentification et droits

## Principe
- **Connexion par session** : `POST /api/auth/connexion` (formulaire `email` + `motDePasse`) dépose un cookie `JSESSIONID`
  (HttpOnly, SameSite=Lax, durée 8 h). Déconnexion : `POST /api/auth/deconnexion`.
- **Mots de passe hachés en BCrypt** (jamais stockés ni renvoyés en clair, 8 à 72 caractères).
- **Protection CSRF** : le serveur dépose un cookie `XSRF-TOKEN` ; tout appel autre que GET doit renvoyer sa valeur
  dans l'en-tête `X-XSRF-TOKEN` (le tableau de bord le fait automatiquement).
- **En-têtes de sécurité** dont une politique CSP (scripts uniquement locaux, tuiles de carte OpenStreetMap autorisées).
- Seules `login.html`, `inscription.html` et `mot-de-passe-oublie.html` (consignes statiques) sont publiques. Sans connexion, l'API répond 401 et les pages redirigent vers `login.html`.

## Inscription et validation des comptes
L'inscription publique (`POST /api/auth/inscription`, page `inscription.html`) crée toujours un **chauffeur inactif** :
personne ne peut s'attribuer un rôle plus élevé. Un administrateur doit **activer** le compte (tableau de bord,
section « Utilisateurs ») avant la première connexion. Un administrateur peut aussi créer directement un compte
actif avec le rôle de son choix.

## Droits par rôle
Chaque rôle a un périmètre distinct : l'**administrateur** équipe et administre, le **superviseur** exploite au quotidien,
le **chauffeur** exécute ses tournées.

| Fonction | Administrateur | Superviseur | Chauffeur |
|---|:-:|:-:|:-:|
| Carte : voir bacs et camions | oui | oui | oui |
| Indicateurs, alertes, zones, liste des chauffeurs | oui | oui | non |
| Traiter une alerte | oui | oui | non |
| Planifier / annuler une tournée | oui | oui | non |
| Voir les tournées | toutes | toutes | **les siennes** |
| Démarrer, vidanger, terminer une tournée | oui | oui | **les siennes** |
| Signaler un incident | oui | oui | **sur ses tournées** |
| Mettre un bac hors service / le remettre en service | oui | oui | non |
| Déclarer un camion en panne / le remettre à disposition | oui | oui | non |
| **Installer, modifier, supprimer un bac** | **oui** | non | non |
| Créer, modifier, supprimer une zone | oui | non | non |
| Créer, modifier, supprimer un camion | oui | non | non |
| Rapports et export CSV | oui | oui | non |
| Gérer les comptes (lister, créer, activer, désactiver, réinitialiser un mot de passe) | oui | non | non |
| Changer son propre mot de passe | oui | oui | oui |

Les droits sont appliqués **côté serveur** (`SecurityConfig` et `TourneeFacade`) ; masquer un bouton dans la page n'est qu'un confort.
Le test `DroitsParRoleTest` vérifie cette matrice route par route pour les trois rôles : il échoue si un droit change sans que
la matrice soit mise à jour.

## Endpoints
| Méthode | URL | Accès |
|---|---|---|
| POST | `/api/auth/connexion` | public (formulaire) |
| POST | `/api/auth/deconnexion` | connecté |
| POST | `/api/auth/inscription` | public |
| GET | `/api/auth/moi` | connecté : identité et rôle |
| POST | `/api/auth/mot-de-passe` | connecté : changer son mot de passe |
| GET | `/api/utilisateurs` | administrateur |
| POST | `/api/utilisateurs` | administrateur : `{role, nom, prenom, email, motDePasse, numeroPermis}` (permis obligatoire pour un chauffeur) |
| POST | `/api/utilisateurs/{id}/activer` | administrateur |
| POST | `/api/utilisateurs/{id}/desactiver` | administrateur (pas son propre compte) |
| POST | `/api/utilisateurs/{id}/reinitialiser-mot-de-passe` | administrateur (pas son propre compte) : renvoie `{motDePasse}` temporaire, affiché une seule fois |

Codes d'erreur : 401 non connecté ou identifiants invalides, 403 droits insuffisants ou jeton CSRF absent,
409 email déjà utilisé, 400 données invalides, 429 trop de tentatives de connexion.

## Limitation des essais de connexion
Après **5 échecs** consécutifs pour une même adresse e-mail, toute connexion à cette adresse est refusée pendant
**15 minutes**, même avec le bon mot de passe : réponse `429` avec l'en-tête `Retry-After` et un message en français.
Un succès remet le compteur à zéro. Les adresses inconnues sont comptées comme les autres, afin de ne pas révéler
quels comptes existent. Un compte inactif (bon mot de passe mais en attente de validation) n'est pas compté.
Réglages : `app.securite.max-echecs` et `app.securite.duree-blocage-minutes` (`application.properties`).
Classes : `LimiteurConnexions`, `FiltreLimitationConnexions`, branchées dans `SecurityConfig`.

## Comptes de démonstration
Créés avec `--app.init-demo=true` : `admin@tulear.mg`, `superviseur@tulear.mg`, `chauffeur@tulear.mg`
(même mot de passe, affiché une seule fois dans les logs, ou fixé par `DEMO_PASSWORD`).

**Mot de passe de démo oublié** :
```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--app.init-demo=true --app.reset-demo-passwords=true"
```
(ajouter `--app.demo-password=...` pour choisir la valeur, sinon elle est générée et affichée).

## Mot de passe oublié
- **Chauffeur ou superviseur** : un administrateur clique sur « Réinitialiser le mot de passe » (section « Utilisateurs »).
  Un mot de passe temporaire aléatoire de 12 caractères s'affiche une seule fois ; l'administrateur le communique
  et l'utilisateur le change avec le bouton « Mot de passe ». Seul le hash BCrypt est conservé.
- **Administrateur** (personne d'autre pour le débloquer) : commande de secours, depuis le serveur :
  ```bash
  mvn spring-boot:run -Dspring-boot.run.arguments="--app.reset-password-email=admin@tulear.mg"
  ```
  Ajouter `--app.new-password=...` pour choisir la valeur, sinon elle est générée et affichée dans la console.
  L'application s'arrête ensuite. Cette commande exige l'accès au serveur et à `.env`.

## Limites connues
- Limitation des essais en mémoire, par adresse e-mail (pas par adresse IP) : un attaquant peut donc bloquer 15 minutes un compte connu, et le compteur repart à zéro au redémarrage. Suffisant pour une seule instance.
- Pas de réinitialisation du mot de passe par e-mail (réinitialisation par un administrateur ou commande de secours). Le mot de passe temporaire n'est pas à changer obligatoirement à la première connexion.
- Derrière HTTPS, activer `server.servlet.session.cookie.secure=true`.
- Un message distinct indique qu'un compte existe mais est inactif ; l'email d'un compte existant est aussi révélé à l'inscription.
