# Architecture et traçabilité

## Vue d'ensemble
Application Java 17 / Spring Boot en couches, base PostgreSQL hébergée sur Supabase, matériel entièrement simulé.
Voir le diagramme de déploiement (`docs/uml/images/deploiement.png`).

| Couche | Paquet | Rôle |
|---|---|---|
| Présentation | `resources/static` | Tableau de bord (HTML, CSS, JavaScript sans framework), pages de connexion et d'inscription |
| API | `api`, `api.dto` | Contrôleurs REST, DTO, traduction des erreurs (400, 401, 403, 404, 409) |
| Sécurité | `securite` | Spring Security : session, CSRF, droits par rôle, CSP |
| Services | `service` | Cas d'usage : tournées, mesures, alertes, comptes, rapports |
| Planification | `planification` | Sélection des bacs et ordre de passage (pattern Strategy) |
| Simulation | `simulation` | Capteurs, communication, pannes, purge des mesures |
| Domaine | `domain` | Entités JPA et règles métier portées par les objets |
| Accès aux données | `repository` | Spring Data JPA ; schéma géré par `supabase/migrations` |

Principes : les règles de gestion vivent dans le **domaine** (testables sans base) ; les contrôleurs ne contiennent pas de logique ;
les DTO sont construits dans la transaction (`open-in-view` désactivé) ; Hibernate est en `ddl-auto=validate` : seul le dossier
`supabase/migrations` modifie le schéma.

## Patterns de conception
| Pattern | Où | Intérêt |
|---|---|---|
| Strategy | `StrategieOptimisation` : `PlusProcheVoisin`, `PrioriteRemplissage` | Changer l'ordre de passage sans toucher au planificateur (`tournee.strategie`) |
| Observer | `ServiceAlerte` et `Observateur` | Ajouter un canal de notification sans modifier la création des alertes |
| Factory | `CapteurFactory` | Créer le bon type de capteur |
| Héritage / polymorphisme | `Utilisateur` (3 rôles), `Capteur` (3 types) | `mesurer()` et `getRole()` propres à chaque sous-classe |
| Façade | `TourneeFacade` | Point d'entrée de l'API, contrôle d'accès par propriétaire |
| Composition | `Bac` ↔ `Capteur`, `Tournee` ↔ `Collecte` | Cycle de vie commun |

## Traçabilité des règles de gestion
| Règle | Où elle est appliquée | Tests |
|---|---|---|
| RG1 états d'un bac | `Seuils.etatPour`, `Bac.mettreAJourNiveau` | `BacTest` |
| RG2 alerte et collecte à 80 % | `ServiceAlerte`, `PlanificateurTournee` | `ServiceAlerteTest`, `PlanificateurTourneeTest` |
| RG3 capacité du camion | `PlanificateurTournee`, `Camion.charger`, `TourneeService.confirmerCollecte` | `PlanificateurTourneeTest`, `TourneeServiceTest` |
| RG4 une tournée active par camion / chauffeur | `TourneeService.planifier` + index uniques partiels (migration `collecte`) | `TourneeServiceTest` |
| RG5 bacs critiques prioritaires | `PlanificateurTournee` (tri par remplissage) | `PlanificateurTourneeTest` |
| RG6 capteur muet | `SurveillanceCapteurs`, `PannesSimulees` | `SurveillanceCapteursTest`, `PannesSimuleesTest` |
| RG7 la vidange remet le bac à 0 | `Collecte.confirmer`, `Bac.vider` | `BacTest`, `TourneeServiceTest` |
| RG8 statuts d'une tournée | `Tournee.demarrer / terminer / annuler` | `TourneeServiceTest` |
| RG9 inscription = chauffeur inactif | `UtilisateurService.inscrire` | `UtilisateurServiceTest`, `SecuriteApiTest` |
| RG10 droits par rôle, tournées du chauffeur | `SecurityConfig`, `TourneeFacade` | `SecuriteApiTest`, `TourneeFacadeTest`, `RapportApiTest` |

Chaque règle a été vérifiée en plus par des essais réels sur un PostgreSQL jetable avec les mêmes migrations que Supabase.

## Choix techniques notables
- **Insertion groupée des mesures** (`MesureBatchWriter`) : la base est distante, un aller-retour par tick au lieu de un par mesure.
- **Purge automatique** des mesures anciennes (`simulation.retention-jours`) : le plan gratuit de Supabase est limité à 500 Mo.
- **Cache mémoire des alertes ouvertes** : évite une requête SQL par mesure ; à invalider quand une alerte est traitée.
- **Fuseau de Madagascar** pour découper les rapports par jour.
- **Leaflet et Chart.js embarqués** (`static/vendor`) : l'interface fonctionne sans CDN (seules les tuiles de carte demandent Internet).

## Limites connues
- Pas d'écran de création ou de modification des bacs, zones et camions (données de démonstration seulement).
- Distances à vol d'oiseau (pas de routage routier) ; position du camion mise à jour à chaque vidange.
- Poids collecté estimé à partir du niveau (densité 0,15 kg/L) ; matériel simulé.
- Pas de limitation des essais de connexion, de réinitialisation du mot de passe par e-mail, ni de HTTPS intégré.
- Mise à jour du tableau de bord par interrogation toutes les 5 secondes (pas de WebSocket).
- Pas de test automatisé du navigateur : l'interface a été vérifiée par captures et scripts Chrome en mode headless.
