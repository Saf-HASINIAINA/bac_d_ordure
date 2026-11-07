-- Alertes et paramètres (seuils RG1/RG2/RG6)

create table alerte (
  id            bigint generated always as identity primary key,
  type          text        not null
                check (type in ('BAC_PLEIN', 'RISQUE_INCENDIE', 'CAPTEUR_PANNE', 'INCIDENT_CHAUFFEUR')),
  message       text        not null,
  bac_id        bigint      references bac(id) on delete set null,
  date_creation timestamptz not null default now(),
  traitee       boolean     not null default false
);

create index idx_alerte_non_traitee on alerte(date_creation desc) where not traitee;
create index idx_alerte_bac         on alerte(bac_id);

-- Paramètres modifiables par l'administrateur (clé / valeur)
create table parametre (
  cle         text primary key,
  valeur      double precision not null,
  description text
);

insert into parametre (cle, valeur, description) values
  ('seuil_moyen_pct',            40, 'Niveau à partir duquel un bac est MOYEN (RG1)'),
  ('seuil_plein_pct',            70, 'Niveau à partir duquel un bac est PLEIN (RG1)'),
  ('seuil_critique_pct',         90, 'Niveau à partir duquel un bac est CRITIQUE (RG1)'),
  ('seuil_alerte_collecte_pct',  80, 'Niveau déclenchant l''alerte BAC_PLEIN (RG2)'),
  ('delai_capteur_panne_min',    30, 'Minutes sans mesure avant alerte CAPTEUR_PANNE (RG6)'),
  ('seuil_gaz_incendie',        300, 'Valeur gaz/température déclenchant RISQUE_INCENDIE');
