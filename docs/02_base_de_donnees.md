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

## 5. Tables (10)
`zone`, `utilisateur`, `camion`, `bac`, `capteur`, `mesure`, `tournee`, `collecte`, `alerte`, `parametre`
