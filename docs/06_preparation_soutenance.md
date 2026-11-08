# Rapport final et soutenance

## Plan proposé pour le rapport
| Chapitre | Contenu | Où trouver la matière |
|---|---|---|
| 1. Introduction | Contexte de Tuléar, problème (tournées fixes, bacs qui débordent), objectif | `01_analyse.md` §1 |
| 2. Cahier des charges | Périmètre, acteurs, besoins fonctionnels et non fonctionnels, règles de gestion RG1 à RG10 | `01_analyse.md` §2 à §6 |
| 3. Modélisation | Cas d'utilisation, classes, séquences, états, activité | `uml/images/` |
| 4. Base de données | Modèle (10 tables), choix de l'héritage en table unique, index uniques partiels, migrations Supabase, RLS | `uml/images/modele_donnees.png`, `02_base_de_donnees.md` |
| 5. Conception orientée objet | Héritage, polymorphisme, Strategy, Observer, Factory, façade ; architecture en couches | `05_architecture.md` |
| 6. Simulation du matériel | Capteurs, communication avec pertes, pannes, accélération du temps | `01_analyse.md` §2, `README.md` |
| 7. Réalisation | API REST, tableau de bord, planification, rapports, authentification | `03_api.md`, `04_authentification.md` |
| 8. Tests et validation | 170+ tests automatisés (dont la matrice complète des droits), essais sur PostgreSQL jetable, captures | `05_architecture.md` (traçabilité) |
| 9. Limites et perspectives | Voir ci-dessous | `05_architecture.md` |
| 10. Conclusion | Bilan | |

Les images des diagrammes sont dans `docs/uml/images/` (régénérables avec PlantUML depuis les fichiers `.puml`).

## Démonstration (10 minutes environ)
Préparer avant : application lancée avec la simulation accélérée,
`mvn spring-boot:run -Dspring-boot.run.arguments="--simulation.intervalle-ms=5000 --simulation.facteur-vitesse=3"`,
trois onglets de navigateur (ou fenêtres privées) pour les trois rôles.

1. **Inscription** : ouvrir `inscription.html`, créer un chauffeur ; tenter de se connecter : refus « en attente de validation ».
2. **Administrateur** : se connecter, menu *Utilisateurs* : activer le compte, créer un superviseur.
3. **Installer un bac** : menu *Bacs*, « + Installer un bac », cliquer sur la carte pour choisir l'emplacement ; le bac apparaît sur le tableau de bord et se remplit.
4. **Parc** : *Zones* et *Camions* (ajouter, modifier) ; montrer qu'un bac déjà collecté ne peut pas être supprimé (on le met hors service).
5. **Tableau de bord** : bacs de couleurs différentes, alertes qui apparaissent, graphique de répartition.
6. **Superviseur** : montrer que son menu n'a ni *Zones* ni *Utilisateurs*, qu'il ne peut pas installer de bac mais peut le mettre hors service ; planifier une tournée dans une zone qui a des bacs pleins ; trajet en pointillés sur la carte.
7. **Chauffeur** : menu réduit à « Tableau de bord », « Mes tournées » et « Mon compte » ; démarrer, confirmer les vidanges (le bac repasse au vert, l'alerte disparaît), **signaler un incident**.
8. **Droits** : taper `#/utilisateurs` dans l'adresse en étant chauffeur : renvoi au tableau de bord.
9. **Rapport** : tonnage, graphique par jour, par zone ; exporter le CSV et l'ouvrir dans un tableur.
10. **Capteurs en panne** (facultatif) : relancer avec `--simulation.taux-panne=0.01 --simulation.duree-panne-ticks=400` et un délai court dans la table `parametre`.
11. **Code** : ouvrir `PlanificateurTournee` et `StrategieOptimisation` (Strategy), `ServiceAlerte` (Observer), `SecurityConfig` (droits), lancer `mvn test` (le test `DroitsParRoleTest` vérifie chaque route pour chaque rôle).

## Questions probables
- **Pourquoi des simulateurs ?** Pas de matériel : chaque capteur est une classe qui produit ses mesures ; remplacer par du vrai matériel ne changerait que la source des mesures (`Capteur.mesurer()`).
- **Comment l'ordre de passage est-il choisi ?** Sélection par priorité de remplissage dans la limite de la capacité du camion, puis ordre par la stratégie (plus proche voisin par défaut). Ce n'est pas l'optimum mathématique (problème du voyageur de commerce) mais une heuristique simple et rapide.
- **Pourquoi la base est distante et comment les changements de schéma sont-ils faits ?** Supabase (PostgreSQL) ; uniquement par migrations versionnées (`supabase db push`), Hibernate ne fait que valider.
- **Comment est protégée l'application ?** BCrypt, session HttpOnly, CSRF, CSP, droits vérifiés côté serveur, inscription qui ne donne jamais de droits.
- **Qu'est-ce qui n'est pas fait ?** Signalements citoyens (optionnel) ; trajet GPS continu et routage routier ; limitation des essais de connexion ; réinitialisation du mot de passe par e-mail.

## Avant la soutenance
- [ ] Vérifier que `.env` contient la bonne URL et le bon mot de passe, et que le compte de démo fonctionne.
- [ ] Tester la démonstration complète une fois, avec la connexion Internet qui sera disponible (les tuiles de carte en ont besoin).
- [ ] Avoir une copie des diagrammes (`docs/uml/images/`) et du dépôt GitHub.
- [ ] Changer le mot de passe de la base si elle a été partagée (Supabase > Settings > Database).
