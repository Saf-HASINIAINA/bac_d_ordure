# API REST

Base : `http://localhost:8080/api` — réponses en JSON. **Toutes les routes exigent une connexion** (sauf inscription et connexion), et les écritures un jeton CSRF :
voir [04_authentification.md](04_authentification.md) pour les droits par rôle.

## Consultation
| Méthode | URL | Description |
|---|---|---|
| GET | `/zones` | Zones (quartiers) |
| GET | `/camions` | Camions, statut, charge, position |
| GET | `/chauffeurs` | Chauffeurs actifs |
| GET | `/configuration` | Position du dépôt des camions |
| GET | `/bacs?zoneId=&etat=` | Bacs, filtrables par zone et état (`VIDE`, `MOYEN`, `PLEIN`, `CRITIQUE`, `HORS_SERVICE`) |
| GET | `/bacs/{id}` | Détail d'un bac |
| GET | `/alertes?traitee=false` | Alertes (toutes, ou seulement à traiter / traitées) |
| GET | `/statistiques` | Bacs par état, taux de remplissage moyen, alertes ouvertes, tournées en cours, tonnage collecté |
| GET | `/tournees?statut=` | Tournées, filtrables (`PLANIFIEE`, `EN_COURS`, `TERMINEE`, `ANNULEE`) |
| GET | `/tournees/{id}` | Détail d'une tournée avec ses collectes dans l'ordre de passage |

## Rapports (administrateur et superviseur)
| Méthode | URL | Description |
|---|---|---|
| GET | `/rapports?debut=&fin=` | Bilan de la période (dates `AAAA-MM-JJ`, inclusives ; 30 derniers jours par défaut, 366 jours maximum) : tonnage, vidanges, tournées terminées, kilomètres, remplissage moyen à la collecte, alertes par type, détail par zone et par jour |
| GET | `/rapports/export.csv?debut=&fin=` | Téléchargement CSV, une ligne par vidange (séparateur `;`, virgule décimale, UTF-8 avec BOM) |

## Actions
| Méthode | URL | Description |
|---|---|---|
| POST | `/tournees` | Planifie une tournée. Corps : `{"zoneId":1,"camionId":1,"chauffeurId":3,"superviseurId":2}` (`superviseurId` facultatif). Réponse 201 |
| POST | `/tournees/{id}/demarrer` | PLANIFIEE -> EN_COURS |
| POST | `/tournees/{id}/collectes/{bacId}/confirmer` | Vide le bac, charge le camion, clôture l'alerte `BAC_PLEIN` |
| POST | `/tournees/{id}/terminer` | EN_COURS -> TERMINEE, camion déchargé et disponible |
| POST | `/tournees/{id}/annuler` | PLANIFIEE -> ANNULEE |
| POST | `/alertes/{id}/traiter` | Marque une alerte comme traitée |

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
