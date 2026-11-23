#!/bin/sh
# Applique les migrations de supabase/migrations sur la base du docker-compose, une seule fois chacune.
# (En production sur Supabase, on utilise `npx supabase db push` : voir docs/02_base_de_donnees.md.)
set -eu

psql -v ON_ERROR_STOP=1 -q <<'SQL'
-- rôles que Supabase fournit d'office et que les migrations (RLS) référencent
do $$ begin
  if not exists (select from pg_roles where rolname = 'anon') then create role anon nologin; end if;
  if not exists (select from pg_roles where rolname = 'authenticated') then create role authenticated nologin; end if;
end $$;
create table if not exists migrations_appliquees (nom text primary key, date_application timestamptz not null default now());
SQL

for fichier in $(ls /migrations/*.sql | sort); do
  nom=$(basename "$fichier")
  deja=$(psql -tA -c "select count(*) from migrations_appliquees where nom = '$nom'")
  if [ "$deja" = "0" ]; then
    echo "Migration appliquée : $nom"
    psql -v ON_ERROR_STOP=1 -q -1 -f "$fichier" -c "insert into migrations_appliquees (nom) values ('$nom')"
  else
    echo "Déjà appliquée    : $nom"
  fi
done
echo "Base à jour."
