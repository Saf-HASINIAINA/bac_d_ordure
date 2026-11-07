-- Sécurité : Supabase expose automatiquement le schéma public via l'API REST (PostgREST).
-- L'application Spring Boot se connecte directement en JDBC avec le rôle postgres
-- (qui contourne la RLS). On active donc la RLS SANS politique : l'API publique
-- (clé anon / authenticated) n'a accès à aucune donnée, seul le backend y accède.

alter table zone         enable row level security;
alter table utilisateur  enable row level security;
alter table camion       enable row level security;
alter table bac          enable row level security;
alter table capteur      enable row level security;
alter table mesure       enable row level security;
alter table tournee      enable row level security;
alter table collecte     enable row level security;
alter table alerte       enable row level security;
alter table parametre    enable row level security;

revoke all on all tables    in schema public from anon, authenticated;
revoke all on all sequences in schema public from anon, authenticated;
