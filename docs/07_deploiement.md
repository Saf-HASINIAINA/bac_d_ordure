# Déploiement avec Docker

Deux usages : la **démonstration locale** (tout dans Docker, une commande) et la **mise en ligne** (l'image de
l'application avec la base Supabase).

## 1. Démonstration locale : base + application
Prérequis : Docker avec le plugin Compose.

```bash
cp .env.docker.example .env.docker      # puis remplir POSTGRES_PASSWORD et DEMO_PASSWORD (8 à 72 caractères)
docker compose --env-file .env.docker up --build
```
Ouvrir http://localhost:8080/ (ou `APP_PORT` de `.env.docker`) et se connecter avec `admin@tulear.mg`,
`superviseur@tulear.mg` ou `chauffeur@tulear.mg` et le mot de passe `DEMO_PASSWORD`.

Ce que fait `docker-compose.yml`, dans l'ordre :
1. `db` : PostgreSQL 15, données conservées dans le volume `donnees-db`.
2. `migration` : applique les fichiers de `supabase/migrations` **une seule fois chacun** (table `migrations_appliquees`),
   après avoir créé les rôles `anon` et `authenticated` que Supabase fournit d'office. Une nouvelle migration est donc
   appliquée au prochain `up`.
3. `app` : l'application, avec `APP_INIT_DEMO=true` (zones, camions, bacs et comptes de démo créés seulement si les tables sont vides).

| Besoin | Commande |
|---|---|
| Arrêter, en gardant les données | `docker compose --env-file .env.docker down` |
| Tout effacer (base comprise) | `docker compose --env-file .env.docker down -v` |
| Voir les logs de l'application | `docker compose --env-file .env.docker logs -f app` |
| Reconstruire après une modification du code | `docker compose --env-file .env.docker up --build` |
| Mot de passe oublié (n'importe quel compte) | `docker compose --env-file .env.docker run --rm app --app.reset-password-email=admin@tulear.mg --server.port=0` : un mot de passe est généré et affiché dans la console (ou ajouter `--app.new-password=...`) ; voir `docs/04_authentification.md` |

`.env.docker` est ignoré par git ; seul `.env.docker.example` (sans secret) est versionné.

## 2. Image seule, avec Supabase
```bash
docker build -t ramassage .
docker run -d --name ramassage -p 8080:8080 \
  -e DB_URL='jdbc:postgresql://<hôte>:5432/postgres' -e DB_USER='<utilisateur>' -e DB_PASSWORD='<mot de passe>' \
  ramassage
```
Les variables sont celles du fichier `.env` (voir `README.md`). Appliquer d'abord les migrations sur Supabase
(`npx supabase db push`, voir `docs/02_base_de_donnees.md`) ; première fois seulement, ajouter `-e APP_INIT_DEMO=true`
pour créer les données de démonstration.

L'image : construction en deux étapes (Maven puis JRE 17 seul), exécution par un utilisateur non privilégié, vérification
d'état (`HEALTHCHECK` sur `/login.html`), mémoire de la JVM bornée à 75 % de celle du conteneur. Aucun secret n'est copié
dans l'image (`.env` est exclu par `.dockerignore`).

## 3. Avant une vraie mise en ligne
- **HTTPS** : l'application ne le fournit pas. La placer derrière un proxy (Caddy, Nginx, Traefik) qui termine le TLS,
  puis ajouter `-e SERVER_SERVLET_SESSION_COOKIE_SECURE=true` pour que le cookie de session ne circule qu'en HTTPS.
- Choisir de vrais mots de passe (jamais ceux de démonstration) et changer ceux des comptes de démo.
- Une seule instance : la limitation des essais de connexion est en mémoire (voir `docs/04_authentification.md`).
- Le plan gratuit de Supabase est limité à 500 Mo : la simulation purge les mesures après 2 jours.
