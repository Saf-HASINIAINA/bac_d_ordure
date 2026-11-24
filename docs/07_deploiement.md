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

## 4. Choix des migrations (pourquoi pas Flyway)
Deux chemins existent, volontairement séparés :
- **Supabase (réel)** : `npx supabase db push` applique les fichiers de `supabase/migrations`. Rien n'est lancé au démarrage de l'application, qui vérifie seulement le schéma (`ddl-auto=validate`).
- **Docker** : le service `migrations` du `docker-compose.yml` applique les mêmes fichiers sur la base locale.

Un outil de migration intégré à l'application (Flyway ou Liquibase) a été écarté : il imposerait de renommer les fichiers,
de déclarer la base Supabase actuelle comme déjà à jour et d'abandonner `supabase db push`, avec un risque sur la base réelle.
**Évolution possible** : adopter Flyway, avec une ligne de base (`baselineOnMigrate`) posée sur la version actuelle du schéma.

## 5. Hébergeur gratuit : Render + Supabase
Architecture : l'application tourne dans un conteneur **Render** (offre gratuite, HTTPS fourni) et garde la base **Supabase**
déjà utilisée en local. Le fichier `render.yaml` décrit le service ; il ne contient aucun secret.

**Étapes (une fois, dans le navigateur)**
1. Créer un compte sur render.com (connexion avec GitHub), puis *New > Blueprint*, choisir le dépôt `bac_d_ordure`.
2. Render lit `render.yaml` et demande les trois valeurs secrètes : `DB_URL`, `DB_USER`, `DB_PASSWORD`. Les recopier depuis votre
   `.env` local (Supabase > Connect > *Session pooler*). Ne pas les écrire ailleurs.
3. *Apply* : Render construit l'image (5 à 10 minutes la première fois) puis donne une adresse `https://ramassage-tulear.onrender.com`.
4. Les migrations restent appliquées depuis votre poste (`./lancer.sh` ou `npx supabase db push`) : l'hébergeur ne touche pas au schéma.
   Les comptes de démonstration existent déjà dans la base Supabase, la connexion fonctionne donc telle quelle.

**Ce que l'offre gratuite impose**
- **Mise en veille après 15 minutes sans visite** : le premier accès suivant prend environ une minute (réveil + démarrage de Spring Boot).
  **La simulation des capteurs est à l'arrêt pendant la veille** : les bacs ne se remplissent plus tant que personne n'ouvre le site.
  Pour la démonstration, ouvrir le site 2 minutes avant. Un service de surveillance gratuit (ex. UptimeRobot, requête sur `/login.html`
  toutes les 5 minutes) l'empêche de dormir ; l'offre gratuite offre 750 heures par mois, de quoi tenir un seul service 24 h/24.
- 512 Mo de mémoire : la JVM est bornée (`JAVA_TOOL_OPTIONS` dans `render.yaml`).
- Une seule instance (la limitation des essais de connexion est en mémoire) : ne pas augmenter le nombre d'instances.
- **Ne pas** arrêter l'application locale et celle de Render en même temps sur la même base : les deux simulent et écrivent des mesures.
  Une seule des deux doit tourner (sinon, `--simulation.actif=false` sur l'une).

**Sécurité** : le cookie de session est réservé à HTTPS (`SERVER_SERVLET_SESSION_COOKIE_SECURE`), l'adresse IP du journal d'audit est
celle du visiteur et non celle du proxy (`SERVER_FORWARD_HEADERS_STRATEGY=framework`). Changer le mot de passe des comptes de démonstration
avant de communiquer l'adresse, et ne jamais publier le mot de passe de la base.
