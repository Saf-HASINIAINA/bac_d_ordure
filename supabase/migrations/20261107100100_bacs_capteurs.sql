-- Bacs, capteurs et mesures

create table bac (
  id                     bigint generated always as identity primary key,
  code                   text             not null unique,
  zone_id                bigint           not null references zone(id),
  latitude               double precision not null check (latitude  between -90  and 90),
  longitude              double precision not null check (longitude between -180 and 180),
  capacite_litres        double precision not null check (capacite_litres > 0),
  niveau_pourcent        double precision not null default 0 check (niveau_pourcent between 0 and 100),
  type_dechet            text             not null default 'MENAGER'
                         check (type_dechet in ('MENAGER', 'RECYCLABLE', 'ORGANIQUE')),
  etat                   text             not null default 'VIDE'
                         check (etat in ('VIDE', 'MOYEN', 'PLEIN', 'CRITIQUE', 'HORS_SERVICE')),
  date_derniere_collecte timestamptz,
  created_at             timestamptz      not null default now()
);

-- Héritage Capteur -> Niveau / Poids / Gaz (table unique, discriminant = type)
create table capteur (
  id              bigint generated always as identity primary key,
  bac_id          bigint      not null references bac(id) on delete cascade,
  type            text        not null check (type in ('NIVEAU', 'POIDS', 'GAZ')),
  actif           boolean     not null default true,
  derniere_mesure timestamptz,
  created_at      timestamptz not null default now(),
  unique (bac_id, type)
);

create table mesure (
  id          bigint generated always as identity primary key,
  capteur_id  bigint           not null references capteur(id) on delete cascade,
  type        text             not null check (type in ('NIVEAU', 'POIDS', 'GAZ', 'TEMPERATURE')),
  valeur      double precision not null,
  horodatage  timestamptz      not null default now()
);

create index idx_bac_zone        on bac(zone_id);
create index idx_bac_niveau      on bac(niveau_pourcent desc);
create index idx_capteur_bac     on capteur(bac_id);
create index idx_mesure_capteur_horodatage on mesure(capteur_id, horodatage desc);
