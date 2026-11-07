-- Données de démonstration (uniquement avec `supabase db reset` en local
-- ou `supabase db push --include-seed` vers le projet distant).
-- Les quartiers et coordonnées sont approximatifs : à vérifier / ajuster pour Tuléar.

insert into zone (nom, frequence_collecte_heures, latitude_centre, longitude_centre) values
  ('Centre-ville',  12, -23.3500, 43.6700),
  ('Betania',       24, -23.3380, 43.6650),
  ('Mahavatse',     24, -23.3600, 43.6850),
  ('Andaboly',      24, -23.3700, 43.6750)
on conflict (nom) do nothing;

insert into camion (immatriculation, capacite_kg, latitude, longitude) values
  ('1234 TAA', 5000, -23.3550, 43.6700),
  ('5678 TAB', 8000, -23.3550, 43.6700)
on conflict (immatriculation) do nothing;

-- Les utilisateurs (avec mots de passe hachés BCrypt) seront créés par l'application
-- au premier démarrage (voir le futur DataInitializer), pas en SQL.
