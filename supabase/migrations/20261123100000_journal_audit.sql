-- Journal d'audit : qui a fait quoi, et quand (actions de modification et connexions).
-- Pas de clé étrangère vers utilisateur : l'historique doit survivre à un compte désactivé ou supprimé,
-- d'où aussi l'e-mail conservé tel quel au moment de l'action.

create table journal_audit (
  id             bigint generated always as identity primary key,
  date_action    timestamptz not null default now(),
  utilisateur_id bigint,
  email          text        not null,
  action         text        not null,
  methode        text        not null,
  chemin         text        not null,
  statut         integer     not null,
  adresse_ip     text
);

create index idx_journal_audit_date on journal_audit(date_action desc);

-- Même politique que les autres tables : le backend (rôle postgres) y accède, l'API publique non.
alter table journal_audit enable row level security;
revoke all on journal_audit from anon, authenticated;
