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
| 8. Tests et validation | 130+ tests automatisés, essais sur PostgreSQL jetable, captures | `05_architecture.md` (traçabilité) |
| 9. Limites et perspectives | Voir ci-dessous | `05_architecture.md` |
| 10. Conclusion | Bilan | |

Les images des diagrammes sont dans `docs/uml/images/` (régénérables avec PlantUML depuis les fichiers `.puml`).

## Démonstration (10 minutes environ)
Préparer avant : application lancée avec la simulation accélérée,
`mvn spring-boot:run -Dspring-boot.run.arguments="--simulation.intervalle-ms=5000 --simulation.facteur-vitesse=3"`,
trois onglets de navigateur (ou fenêtres privées) pour les trois rôles.

1. **Inscription** : ouvrir `inscription.html`, créer un chauffeur ; tenter de se connecter : refus « en attente de validation ».
2. **Administrateur** : se connecter, valider le compte dans « Utilisateurs », créer un superviseur.
3. **Carte** : bacs de couleurs différentes, alertes qui apparaissent, graphique de répartition.
4. **Superviseur** : planifier une tournée dans une zone qui a des bacs pleins ; montrer le trajet en pointillés et l'ordre de passage.
5. **Chauffeur** : se connecter (ne voit que sa tournée, pas de formulaire de planification), démarrer, confirmer les vidanges : le bac repasse au vert, l'alerte disparaît, le camion bouge.
6. **Droits** : montrer qu'un chauffeur n'a pas accès aux rapports ni aux comptes (403).
7. **Rapport** : tonnage, graphique par jour, par zone ; exporter le CSV et l'ouvrir dans un tableur.
8. **Capteurs en panne** (facultatif) : relancer avec `--simulation.taux-panne=0.01 --simulation.duree-panne-ticks=400` et un délai court dans la table `parametre` : une alerte « Capteur en panne » apparaît.
9. **Code** : ouvrir `PlanificateurTournee` et `StrategieOptimisation` (Strategy), `ServiceAlerte` (Observer), lancer `mvn test`.

## Questions probables
- **Pourquoi des simulateurs ?** Pas de matériel : chaque capteur est une classe qui produit ses mesures ; remplacer par du vrai matériel ne changerait que la source des mesures (`Capteur.mesurer()`).
- **Comment l'ordre de passage est-il choisi ?** Sélection par priorité de remplissage dans la limite de la capacité du camion, puis ordre par la stratégie (plus proche voisin par défaut). Ce n'est pas l'optimum mathématique (problème du voyageur de commerce) mais une heuristique simple et rapide.
- **Pourquoi la base est distante et comment les changements de schéma sont-ils faits ?** Supabase (PostgreSQL) ; uniquement par migrations versionnées (`supabase db push`), Hibernate ne fait que valider.
- **Comment est protégée l'application ?** BCrypt, session HttpOnly, CSRF, CSP, droits vérifiés côté serveur, inscription qui ne donne jamais de droits.
- **Qu'est-ce qui n'est pas fait ?** Écrans de création des bacs, zones et camions ; signalement d'incident par le chauffeur ; signalements citoyens ; routage routier ; limitation des essais de connexion.

## Avant la soutenance
- [ ] Vérifier que `.env` contient la bonne URL et le bon mot de passe, et que le compte de démo fonctionne.
- [ ] Tester la démonstration complète une fois, avec la connexion Internet qui sera disponible (les tuiles de carte en ont besoin).
- [ ] Avoir une copie des diagrammes (`docs/uml/images/`) et du dépôt GitHub.
- [ ] Changer le mot de passe de la base si elle a été partagée (Supabase > Settings > Database).
