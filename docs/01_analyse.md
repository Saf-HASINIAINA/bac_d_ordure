# Gestion de ramassage de bacs d'ordures automatique — Commune Urbaine de Tuléar

Projet de fin d'année — M1 ENI Fianarantsoa
Technologies : Java (web, Spring Boot) + PostgreSQL — matériel remplacé par des **simulateurs**.

---

## 1. Contexte et problématique
La collecte des ordures à Tuléar se fait sur des tournées fixes : des bacs débordent
pendant que d'autres sont ramassés presque vides. Cela coûte du carburant, du temps
et dégrade la salubrité de la ville.

**Objectif** : automatiser le ramassage grâce à des bacs « intelligents » qui signalent
leur niveau de remplissage, et planifier dynamiquement des tournées de camions
uniquement vers les bacs qui en ont besoin.

## 2. Périmètre et simulation
Aucun matériel réel : tout le matériel est simulé par des classes Java.

| Matériel réel | Simulateur |
|---|---|
| Capteur ultrason (niveau) | `CapteurNiveau` : le remplissage croît de façon aléatoire selon la zone et l'heure |
| Capteur de poids | `CapteurPoids` |
| Capteur gaz / température (risque incendie, odeurs) | `CapteurGaz` |
| Module GSM / LoRa | `ModuleCommunication` : envoi de mesures avec latence et pertes simulées |
| GPS du camion | `GPSCamion` : déplacement le long de l'itinéraire |
| Pesée / benne du camion | `Camion.chargeActuelle` |

Un `MoteurSimulation` (tâche planifiée) fait avancer le temps simulé, et un accélérateur
permet de faire passer « 1 jour » en quelques minutes pour la démonstration.

## 3. Acteurs
| Acteur | Rôle |
|---|---|
| **Administrateur** | Gère les utilisateurs, zones, bacs, camions et paramètres (seuils). |
| **Superviseur (service de propreté)** | Suit la carte en temps réel, valide/lance les tournées, consulte les rapports. |
| **Chauffeur / équipe de collecte** | Consulte sa tournée, confirme la vidange de chaque bac, signale un incident. |
| **Visiteur** | S'inscrit comme chauffeur (compte en attente de validation) et se connecte. |
| **Citoyen** *(optionnel, non réalisé)* | Signale un bac endommagé ou un dépôt sauvage. |
| **Système de simulation** (acteur secondaire) | Génère les mesures des capteurs et les positions GPS. |

## 4. Besoins fonctionnels
- **BF1** Gérer les bacs (CRUD, position GPS, capacité, zone, type de déchet). — *réalisé : l'administrateur installe, modifie et supprime un bac (supprimable tant qu'il n'a jamais été collecté) ; le superviseur le met hors service*
- **BF2** Gérer les zones/quartiers de Tuléar. — *réalisé : l'administrateur crée, modifie et supprime une zone (si elle est vide et sans historique)*
- **BF3** Gérer les camions et les chauffeurs. — *réalisé : camions (administrateur ; panne et remise à disposition par le superviseur) et comptes chauffeurs*
- **BF4** Recevoir et enregistrer les mesures des capteurs (niveau, poids, gaz). — *réalisé*
- **BF5** Générer automatiquement des alertes (bac plein, incendie, capteur en panne). — *réalisé : bac plein, risque d'incendie, capteur en panne, incident signalé par un chauffeur*
- **BF6** Planifier automatiquement des tournées optimisées (bacs prioritaires, capacité du camion). — *réalisé (deux stratégies : plus proche voisin, priorité au remplissage)*
- **BF7** Affecter une tournée à un camion et un chauffeur. — *réalisé*
- **BF8** Suivre en temps réel la position des camions et l'avancement de la tournée. — *partiel : la position du camion est mise à jour à chaque vidange, pas de trajet GPS continu*
- **BF9** Confirmer la vidange d'un bac (remet le niveau à zéro, enregistre la collecte). — *réalisé*
- **BF10** Afficher une carte (Leaflet / OpenStreetMap) avec bacs colorés selon le niveau. — *réalisé (Leaflet / OpenStreetMap)*
- **BF11** Produire des rapports et statistiques (tonnage, km parcourus, taux de remplissage à la collecte). — *réalisé (période au choix, par zone et par jour)*
- **BF12** Authentification et gestion des rôles. — *réalisé (3 rôles, session, CSRF, BCrypt)*
- **BF13** Gérer les signalements des citoyens *(optionnel)*. — *non réalisé (optionnel)*

- **BF14** Inscription publique et validation des comptes par un administrateur — *réalisé*.
- **BF15** Export CSV des vidanges — *réalisé*.
- **BF16** Signalement d'un incident par le chauffeur sur sa tournée — *réalisé*.
- **BF17** Changement de son mot de passe — *réalisé*.

## 5. Besoins non fonctionnels
- Sécurité : authentification, mots de passe hachés, contrôle d'accès par rôle.
- Performance : mise à jour de la carte en quasi temps réel (WebSocket ou SSE).
- Maintenabilité : architecture en couches, principes SOLID, tests unitaires.
- Portabilité : application web, Java 17+, PostgreSQL.
- Ergonomie : interface simple, utilisable sur ordinateur et mobile (chauffeur).

## 6. Règles de gestion
- **RG1** Un bac est *Vide* (< 40 %), *Moyen* (40–70 %), *Plein* (70–90 %) ou *Critique* (> 90 %). Seuils paramétrables.
- **RG2** Un bac à plus de 80 % déclenche une alerte « à collecter ».
- **RG3** Une tournée ne dépasse pas la capacité du camion.
- **RG4** Un camion et un chauffeur ne peuvent avoir qu'une tournée active à la fois.
- **RG5** Un bac critique ou en risque d'incendie est prioritaire sur le reste.
- **RG6** Un capteur sans mesure depuis plus de N minutes est déclaré en panne (alerte).
- **RG7** La confirmation de collecte remet le niveau du bac à 0 et enregistre le poids collecté.
- **RG8** Une tournée a le statut : *Planifiée → En cours → Terminée* (ou *Annulée*).
- **RG9** L'inscription publique crée toujours un chauffeur inactif ; seul un administrateur peut l'activer ou créer un compte avec un autre rôle.
- **RG10** Droits par rôle (voir `04_authentification.md`) : un chauffeur ne voit et ne manipule que les tournées qui lui sont confiées ; planifier, annuler, traiter une alerte, consulter les rapports sont réservés aux superviseurs et administrateurs ; installer ou retirer un bac, une zone ou un camion est réservé à l'administrateur.
- **RG11** Un bac, une zone ou un camion qui a un historique (collectes, tournées) ne peut pas être supprimé : on le met hors service. Un bac prévu dans une tournée en cours ne peut pas être mis hors service.

## 7. Choix techniques
| Couche | Choix |
|---|---|
| Langage | Java 17+ |
| Framework | Spring Boot (MVC, Data JPA, Security, WebSocket) |
| Vue | Thymeleaf + Leaflet.js + Chart.js |
| Base de données | PostgreSQL (PostGIS optionnel) |
| Build | Maven |
| Tests | JUnit 5, Mockito |
| Modélisation | UML (PlantUML), MCD/MLD |

## 8. Concepts orientés objet mis en œuvre
- **Abstraction / héritage** : `Utilisateur` → `Administrateur`, `Superviseur`, `Chauffeur` ; `Capteur` → `CapteurNiveau`, `CapteurPoids`, `CapteurGaz`.
- **Polymorphisme** : `Capteur.mesurer()`, `StrategieOptimisation.calculer()`.
- **Interfaces** : `StrategieOptimisation`, `Observateur`.
- **Encapsulation** : attributs privés, validation dans les setters.
- **Composition / agrégation** : `Bac` possède ses `Capteur` ; `Tournee` agrège des `Collecte`.
- **Patterns** : *Strategy* (algorithme de tournée), *Observer* (alertes), *Factory* (création de capteurs), *Repository/DAO* (persistance), *Singleton* (moteur de simulation).

## 9. Avancement
| Étape | État |
|---|---|
| Analyse et UML | fait |
| Modèle de données et migrations Supabase | fait |
| Noyau métier et simulateurs | fait |
| Algorithme de tournée et alertes | fait |
| API REST et tableau de bord web (carte) | fait |
| Authentification, inscription, droits par rôle | fait |
| Rapports et export CSV | fait |
| Détection des capteurs en panne (RG6) | fait |
| Gestion du parc : installer, modifier, retirer bacs, zones et camions (API) | fait |
| Nouvelle interface à barre latérale et pages par rôle | voir la PR suivante |
| Signalement d'incident par le chauffeur | fait |
| Signalements citoyens | non réalisé (optionnel) |
| Rapport final et soutenance | voir `06_preparation_soutenance.md` |

## 10. Dossier des diagrammes
Sources PlantUML dans `docs/uml/`, images dans `docs/uml/images/` :
- `cas_utilisation` : acteurs et cas d'utilisation
- `classes_domaine` : entités du domaine
- `classes_services` : services, patterns (Strategy, Observer, Factory), simulation, sécurité
- `modele_donnees` : tables PostgreSQL
- `sequence_collecte` : de la mesure à la vidange
- `sequence_connexion` : inscription, validation, connexion
- `activite_tournee`, `etats_bac`, `etats_tournee`
- `deploiement` : architecture de déploiement
