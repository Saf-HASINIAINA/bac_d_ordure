# Ramassage automatique de bacs — Commune Urbaine de Tuléar

Projet de fin d'année M1 — ENI Fianarantsoa. Java 17 / Spring Boot / PostgreSQL (Supabase), matériel simulé.

## Lancer l'application
1. `cp .env.example .env` puis compléter `DB_URL` (Supabase > Connect > *Session pooler*) et `DB_PASSWORD`.
2. Première fois seulement, pour créer zones, camions, bacs, capteurs et comptes de démo :
   `mvn spring-boot:run -Dspring-boot.run.arguments=--app.init-demo=true`
   (le mot de passe des comptes de démo est affiché dans les logs, ou fixé par `DEMO_PASSWORD`).
3. Ensuite : `mvn spring-boot:run`

Réglages de la simulation dans `src/main/resources/application.properties` (`simulation.*`) :
intervalle des mesures (30 s par défaut, `--simulation.intervalle-ms=5000` pour une démo), accélération du temps,
taux de perte des messages et durée de conservation des mesures (2 jours, purge automatique).
Attention : le plan gratuit de Supabase est limité à 500 Mo, arrêtez l'application quand vous ne l'utilisez pas.

## Tests
`mvn test` (tests unitaires, sans base de données).

## Base de données
Schéma géré uniquement par les migrations : voir [docs/02_base_de_donnees.md](docs/02_base_de_donnees.md).

## Structure
- `domain/` entités JPA et logique métier (Bac, Capteur et ses sous-classes, Tournee, ...)
- `service/` alertes (Observer), traitement des mesures, paramètres
- `simulation/` moteur de simulation et module de communication simulé
- `repository/` accès aux données (Spring Data JPA)
- `config/` données de démonstration
