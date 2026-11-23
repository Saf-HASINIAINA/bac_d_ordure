# Base de données — Supabase + migrations

Règle d'or : **on ne modifie jamais le schéma à la main** (ni dans le dashboard, ni via psql).
Tout changement = un nouveau fichier dans `supabase/migrations/`, puis `db push`.

## 1. Première mise en place (une seule fois)
1. Créer un projet sur https://supabase.com (région proche, ex. Europe) et noter le **mot de passe de la base**.
2. Depuis la racine du projet :
   ```bash
   npx supabase login                       # ouvre le navigateur
   npx supabase link --project-ref <REF>    # REF = ID du projet (Settings > General)
   npx supabase db push --dry-run           # prévisualise les migrations à appliquer
   npx supabase db push                     # applique les migrations
   npx supabase db push --include-seed      # (optionnel) + données de démo
   ```

## 2. Modifier le schéma ensuite
```bash
npx supabase migration new ajout_colonne_xxx   # crée un fichier horodaté vide
# écrire le SQL dans ce fichier, puis :
npx supabase db push
```
Ne jamais éditer une migration déjà poussée : en créer une nouvelle.

## 3. Connexion de Spring Boot
Dashboard > **Connect** > *Session pooler* (la connexion directe est en IPv6 uniquement).
`application.properties` (valeurs réelles dans des variables d'environnement, jamais dans git) :
```properties
spring.datasource.url=jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres
spring.datasource.username=postgres.<REF>
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate   # Hibernate vérifie, ne modifie JAMAIS le schéma
```

## 4. Sécurité
La RLS est activée sans politique sur toutes les tables : l'API REST publique de Supabase
ne voit rien, seul le backend (connexion JDBC) accède aux données.

## 5. Tables (11)
`zone`, `utilisateur`, `camion`, `bac`, `capteur`, `mesure`, `tournee`, `collecte`, `alerte`, `parametre`, `journal_audit`

`journal_audit` (migration `20261123100000_journal_audit.sql`) : date, utilisateur (sans clé étrangère, pour que l'historique survive à un compte supprimé), e-mail, action, méthode, chemin, statut, adresse IP. **Après une mise à jour du code, appliquer cette migration (`npx supabase db push`) avant de relancer l'application** : Hibernate est en mode `validate` et refuse de démarrer si la table manque.

`utilisateur.mdp_a_changer` (migration `20261124100000_mdp_a_changer.sql`) : vrai tant que le mot de passe a été imposé par un administrateur et n'a pas été remplacé (voir `docs/04_authentification.md`).

**Automatique au lancement** : `./lancer.sh` exécute `npx supabase db push --remote` (mot de passe pris dans `DB_PASSWORD` du `.env`) avant de démarrer l'application. Les migrations déjà appliquées sont ignorées ; seul un nouveau fichier est exécuté.
