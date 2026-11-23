-- Changement de mot de passe obligatoire : vrai quand le mot de passe a été fixé par un administrateur
-- (réinitialisation, création de compte) ou par la commande de secours. Remis à faux dès que l'utilisateur le change.
alter table utilisateur add column mdp_a_changer boolean not null default false;
