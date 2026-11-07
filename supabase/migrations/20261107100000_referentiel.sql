-- Référentiel : zones, utilisateurs, camions

create table zone (
  id                        bigint generated always as identity primary key,
  nom                       text        not null unique,
  frequence_collecte_heures integer     not null default 24 check (frequence_collecte_heures > 0),
  latitude_centre           double precision,
  longitude_centre          double precision,
  created_at                timestamptz not null default now()
);

-- Héritage Utilisateur -> Administrateur / Superviseur / Chauffeur
-- stratégie "table unique" (JPA SINGLE_TABLE), discriminant = role
create table utilisateur (
  id                bigint generated always as identity primary key,
  nom               text        not null,
  prenom            text        not null,
  email             text        not null unique,
  mot_de_passe_hache text       not null,
  role              text        not null check (role in ('ADMIN', 'SUPERVISEUR', 'CHAUFFEUR')),
  numero_permis     text,                       -- renseigné uniquement pour CHAUFFEUR
  actif             boolean     not null default true,
  created_at        timestamptz not null default now(),
  constraint chk_permis_chauffeur check (role <> 'CHAUFFEUR' or numero_permis is not null)
);

create table camion (
  id                bigint generated always as identity primary key,
  immatriculation   text             not null unique,
  capacite_kg       double precision not null check (capacite_kg > 0),
  charge_actuelle_kg double precision not null default 0 check (charge_actuelle_kg >= 0),
  statut            text             not null default 'DISPONIBLE'
                    check (statut in ('DISPONIBLE', 'EN_TOURNEE', 'EN_PANNE')),
  latitude          double precision,           -- position GPS simulée
  longitude         double precision,
  created_at        timestamptz      not null default now(),
  constraint chk_charge check (charge_actuelle_kg <= capacite_kg)
);
