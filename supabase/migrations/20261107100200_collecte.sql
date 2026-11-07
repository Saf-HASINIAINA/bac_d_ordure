-- Tournées et collectes

create table tournee (
  id            bigint generated always as identity primary key,
  date          date             not null default current_date,
  statut        text             not null default 'PLANIFIEE'
                check (statut in ('PLANIFIEE', 'EN_COURS', 'TERMINEE', 'ANNULEE')),
  distance_km   double precision not null default 0 check (distance_km >= 0),
  zone_id       bigint           not null references zone(id),
  camion_id     bigint           not null references camion(id),
  chauffeur_id  bigint           not null references utilisateur(id),
  superviseur_id bigint          references utilisateur(id),
  created_at    timestamptz      not null default now()
);

-- RG4 : un camion / un chauffeur ne peut avoir qu'une tournée active à la fois
create unique index uq_tournee_active_camion
  on tournee(camion_id)    where statut = 'EN_COURS';
create unique index uq_tournee_active_chauffeur
  on tournee(chauffeur_id) where statut = 'EN_COURS';

create table collecte (
  id                bigint generated always as identity primary key,
  tournee_id        bigint           not null references tournee(id) on delete cascade,
  bac_id            bigint           not null references bac(id),
  ordre             integer          not null check (ordre > 0),
  poids_collecte_kg double precision check (poids_collecte_kg >= 0),
  heure_collecte    timestamptz,
  effectuee         boolean          not null default false,
  unique (tournee_id, ordre),
  unique (tournee_id, bac_id)
);

create index idx_tournee_statut   on tournee(statut);
create index idx_tournee_camion   on tournee(camion_id);
create index idx_tournee_chauffeur on tournee(chauffeur_id);
create index idx_collecte_bac     on collecte(bac_id);
