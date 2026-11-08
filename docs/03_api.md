# API REST

Base : `http://localhost:8080/api` — réponses en JSON. **Toutes les routes exigent une connexion** (sauf inscription et connexion), et les écritures un jeton CSRF :
voir [04_authentification.md](04_authentification.md) pour les droits par rôle.

## Consultation
| Méthode | URL | Description |
|---|---|---|
| GET | `/zones` | Zones (quartiers) avec leur nombre de bacs — administrateur et superviseur |
| GET | `/camions` | Camions, statut, charge, position |
| GET | `/chauffeurs` | Chauffeurs actifs — administrateur et superviseur |
| GET | `/configuration` | Position du dépôt des camions |
| GET | `/bacs?zoneId=&etat=` | Bacs, filtrables par zone et état (`VIDE`, `MOYEN`, `PLEIN`, `CRITIQUE`, `HORS_SERVICE`) |
| GET | `/bacs/{id}` | Détail d'un bac |
| GET | `/alertes?traitee=false` | Alertes (toutes, ou seulement à traiter / traitées) — administrateur et superviseur |
| GET | `/statistiques` | Bacs par état, taux de remplissage moyen, alertes ouvertes, tournées en cours, tonnage collecté — administrateur et superviseur |
| GET | `/tournees?statut=` | Tournées, filtrables (`PLANIFIEE`, `EN_COURS`, `TERMINEE`, `ANNULEE`) ; un chauffeur ne voit que les siennes |
| GET | `/tournees/{id}` | Détail d'une tournée avec ses collectes dans l'ordre de passage |

## Rapports (administrateur et superviseur)
| Méthode | URL | Description |
|---|---|---|
| GET | `/rapports?debut=&fin=` | Bilan de la période (dates `AAAA-MM-JJ`, inclusives ; 30 derniers jours par défaut, 366 jours maximum) : tonnage, vidanges, tournées terminées, kilomètres, remplissage moyen à la collecte, alertes par type, détail par zone et par jour |
| GET | `/rapports/export.csv?debut=&fin=` | Téléchargement CSV, une ligne par vidange (séparateur `;`, virgule décimale, UTF-8 avec BOM) |

## Gestion du parc
| Méthode | URL | Qui | Description |
|---|---|---|---|
| POST | `/bacs` | administrateur | **Installe un bac** : `{"zoneId":1,"latitude":-23.35,"longitude":43.67,"capaciteLitres":240,"typeDechet":"MENAGER","code":"BAC-100"}` (`code` et `typeDechet` facultatifs : code généré `BAC-025`, type ménager). Le bac est créé vide avec ses trois capteurs (niveau, poids, gaz) qui émettent aussitôt. Réponse 201 |
| PUT | `/bacs/{id}` | administrateur | Modifie zone, position, capacité, type (le code ne change jamais) |
| DELETE | `/bacs/{id}` | administrateur | Supprime un bac **jamais collecté** (204) ; sinon 409 : le mettre hors service |
| POST | `/bacs/{id}/hors-service` | administrateur, superviseur | Maintenance : le bac n'est plus proposé dans les tournées (refusé s'il est prévu dans une tournée en cours) |
| POST | `/bacs/{id}/remise-en-service` | administrateur, superviseur | Fin de maintenance, l'état est recalculé d'après le niveau |
| POST | `/zones` | administrateur | `{"nom":"Mahavatse","frequenceCollecteHeures":24,"latitudeCentre":-23.36,"longitudeCentre":43.68}` (centre facultatif) |
| PUT | `/zones/{id}` | administrateur | Modifie une zone |
| DELETE | `/zones/{id}` | administrateur | Refusé (409) si la zone contient des bacs ou a un historique de tournées |
| POST | `/camions` | administrateur | `{"immatriculation":"1234 TAA","capaciteKg":5000}` ; le camion est placé au dépôt |
| PUT | `/camions/{id}` | administrateur | Modifie immatriculation et capacité (pas sous la charge actuelle) |
| DELETE | `/camions/{id}` | administrateur | Refusé (409) s'il a un historique de tournées |
| POST | `/camions/{id}/panne` | administrateur, superviseur | Déclare un camion en panne (refusé s'il est affecté à une tournée) |
| POST | `/camions/{id}/disponible` | administrateur, superviseur | Remet un camion en panne à disposition |

## Actions
| Méthode | URL | Description |
|---|---|---|
| POST | `/tournees` | Planifie une tournée. Corps : `{"zoneId":1,"camionId":1,"chauffeurId":3,"superviseurId":2}` (`superviseurId` facultatif). Réponse 201 |
| POST | `/tournees/{id}/demarrer` | PLANIFIEE -> EN_COURS |
| POST | `/tournees/{id}/collectes/{bacId}/confirmer` | Vide le bac, charge le camion, clôture l'alerte `BAC_PLEIN` |
| POST | `/tournees/{id}/terminer` | EN_COURS -> TERMINEE, camion déchargé et disponible |
| POST | `/tournees/{id}/annuler` | PLANIFIEE -> ANNULEE |
| POST | `/tournees/{id}/incidents` | Signale un incident : `{"description":"Accès bloqué","bacId":12}` (`bacId` facultatif, doit appartenir à la tournée). Crée une alerte `INCIDENT_CHAUFFEUR`. Chauffeur sur ses tournées, ou superviseur / administrateur. Réponse 201 |
| POST | `/alertes/{id}/traiter` | Marque une alerte comme traitée |
| POST | `/auth/mot-de-passe` | Change son propre mot de passe : `{"ancienMotDePasse":"...","nouveauMotDePasse":"..."}` (8 à 72 caractères) |

## Erreurs
Corps : `{"statut":409,"message":"...","horodatage":"..."}`

| Code | Cas |
|---|---|
| 400 | Requête invalide : champ manquant, JSON illisible, valeur d'énumération inconnue |
| 404 | Ressource introuvable |
| 409 | Règle de gestion violée : camion indisponible, chauffeur déjà occupé, bac déjà vidé, aucun bac à collecter... |

## Exemple
```bash
curl -s "http://localhost:8080/api/bacs?etat=CRITIQUE"
curl -s -X POST -H 'Content-Type: application/json' \
     -d '{"zoneId":1,"camionId":1,"chauffeurId":3}' http://localhost:8080/api/tournees
```
Les identifiants de démonstration (zones, camions, chauffeur) se lisent avec `/zones`, `/camions`
et la table `utilisateur` (le chauffeur de démo est `chauffeur@tulear.mg`).
