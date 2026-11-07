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

## Pannes de capteurs (RG6)
Un capteur qui n'envoie plus de mesure depuis plus de `delai_capteur_panne_min` minutes (30 par défaut, modifiable dans la
table `parametre`) déclenche une alerte « Capteur en panne », une seule par bac tant qu'elle n'est pas traitée.
La simulation provoque de temps en temps de fausses pannes (`simulation.taux-panne`, `simulation.duree-panne-ticks`).
Pour la démonstration : `--simulation.taux-panne=0.01 --simulation.duree-panne-ticks=400` et un délai court dans `parametre`.

## Connexion
Le tableau de bord est protégé : ouvrir http://localhost:8080/ renvoie vers la page de connexion. Les comptes de démo
(`admin@`, `superviseur@`, `chauffeur@tulear.mg`) sont créés avec `--app.init-demo=true`. Un nouveau chauffeur peut
s'inscrire, un administrateur valide ensuite son compte. Détails, droits par rôle et mot de passe oublié :
[docs/04_authentification.md](docs/04_authentification.md).

## Tableau de bord
Une fois connecté, http://localhost:8080/ affiche : carte des bacs (couleur selon le niveau), camions,
trajet de la tournée choisie, indicateurs, alertes à traiter, planification et suivi des tournées.
La page se met à jour toute seule toutes les 5 secondes. Le fond de carte OpenStreetMap demande une connexion Internet ;
le reste fonctionne sans (Leaflet et Chart.js sont embarqués dans `static/vendor/`).

## Planification des tournées
Le planificateur retient les bacs remplis à 80 % ou plus (RG2), en commençant par les plus remplis (RG5), sans dépasser
la capacité du camion (RG3), puis les ordonne avec la stratégie choisie par `tournee.strategie`
(`plus-proche-voisin` par défaut, ou `priorite-remplissage`). Le dépôt se règle avec `tournee.depot-latitude/longitude`.
Cycle d'une tournée : planifier -> démarrer -> confirmer la vidange de chaque bac -> terminer (ou annuler tant qu'elle est planifiée).

## API REST
Les endpoints (bacs, alertes, tournées, statistiques) sont décrits dans [docs/03_api.md](docs/03_api.md).

## Rapports
Réservés aux administrateurs et superviseurs : section « Rapport d'activité » du tableau de bord (période au choix, 30 derniers
jours par défaut). Tonnage collecté (total, par jour, par zone), vidanges, tournées terminées, kilomètres parcourus, remplissage
moyen des bacs au moment de la vidange et alertes déclenchées. Les jours sont découpés selon le fuseau `app.fuseau`
(`Indian/Antananarivo`). L'export CSV (une ligne par vidange, prévu pour Excel en français) est aussi disponible via
`GET /api/rapports/export.csv?debut=AAAA-MM-JJ&fin=AAAA-MM-JJ`.

## Tests
`mvn test` (tests unitaires, sans base de données).

## Base de données
Schéma géré uniquement par les migrations : voir [docs/02_base_de_donnees.md](docs/02_base_de_donnees.md).

## Structure
- `domain/` entités JPA et logique métier (Bac, Capteur et ses sous-classes, Tournee, ...)
- `service/` alertes (Observer), traitement des mesures, paramètres, cycle de vie des tournées (`TourneeService`)
- `planification/` planificateur de tournées et stratégies d'optimisation (Strategy : `PlusProcheVoisin`, `PrioriteRemplissage`)
- `simulation/` moteur de simulation et module de communication simulé
- `resources/static/` tableau de bord (HTML, CSS, JavaScript sans framework)
- `securite/` Spring Security : connexion par session, CSRF, droits par rôle
- `api/` contrôleurs REST, DTO et gestion des erreurs
- `repository/` accès aux données (Spring Data JPA)
- `config/` données de démonstration
