#!/usr/bin/env bash
# Lance l'application en appliquant d'abord les migrations Supabase (npx supabase db push).
# Usage : ./lancer.sh [arguments Spring...]   ex. ./lancer.sh --app.init-demo=true
# Variable SANS_MIGRATION=1 : saute l'étape de migration (hors ligne, démonstration).
set -euo pipefail
cd "$(dirname "$0")"

if [ ! -f .env ]; then
  echo "Fichier .env introuvable : copiez .env.example en .env et complétez-le." >&2
  exit 1
fi
set -a; . ./.env; set +a

if [ "${SANS_MIGRATION:-0}" != "1" ]; then
  if [ -z "${DB_PASSWORD:-}" ]; then
    echo "DB_PASSWORD est vide dans .env." >&2
    exit 1
  fi
  if [ ! -f supabase/.temp/project-ref ]; then
    echo "Projet Supabase non lié. Une seule fois : npx supabase link --project-ref <REF>" >&2
    exit 1
  fi
  echo "==> Migrations Supabase (npx supabase db push)"
  SUPABASE_DB_PASSWORD="$DB_PASSWORD" npx --yes supabase db push --remote
fi

echo "==> Démarrage de l'application"
if [ "$#" -gt 0 ]; then
  exec mvn spring-boot:run -Dspring-boot.run.arguments="$*"
fi
exec mvn spring-boot:run
