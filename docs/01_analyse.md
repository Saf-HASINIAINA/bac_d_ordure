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
| **Citoyen** *(optionnel)* | Signale un bac endommagé ou un dépôt sauvage. |
| **Système de simulation** (acteur secondaire) | Génère les mesures des capteurs et les positions GPS. |

## 4. Besoins fonctionnels
- **BF1** Gérer les bacs (CRUD, position GPS, capacité, zone, type de déchet).
- **BF2** Gérer les zones/quartiers de Tuléar.
- **BF3** Gérer les camions et les chauffeurs.
- **BF4** Recevoir et enregistrer les mesures des capteurs (niveau, poids, gaz).
- **BF5** Générer automatiquement des alertes (bac plein, incendie, capteur en panne).
- **BF6** Planifier automatiquement des tournées optimisées (bacs prioritaires, capacité du camion).
- **BF7** Affecter une tournée à un camion et un chauffeur.
- **BF8** Suivre en temps réel la position des camions et l'avancement de la tournée.
- **BF9** Confirmer la vidange d'un bac (remet le niveau à zéro, enregistre la collecte).
- **BF10** Afficher une carte (Leaflet / OpenStreetMap) avec bacs colorés selon le niveau.
- **BF11** Produire des rapports et statistiques (tonnage, km parcourus, taux de remplissage à la collecte).
- **BF12** Authentification et gestion des rôles.
- **BF13** Gérer les signalements des citoyens *(optionnel)*.

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

## 9. Planning proposé
1. Analyse et UML ← **en cours**
2. Modèle de données (MCD/MLD) et script PostgreSQL
3. Noyau métier + simulateurs (sans web)
4. Algorithme de tournée et alertes
5. Couche web (API REST + pages) et carte
6. Authentification, rapports
7. Tests, rapport final et soutenance

## 10. Dossier des diagrammes
Voir `docs/uml/` :
- `cas_utilisation.puml`
- `classes.puml`
- `sequence_collecte.puml`
- `activite_tournee.puml`
- `etats_bac.puml`
