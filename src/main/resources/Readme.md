# Solution hybride (meilleur choix ?)
1. Pour récupérer les utilisateurs lors d'un changement immédiat → WebClient ou FeignClient
2. Pour envoyer des notifications aux utilisateurs sans attendre une réponse immédiate → Kafka
3. Utilise WebClient (réactif) ou FeignClient (déclaratif)
# Explication du processus
Création d'un nouvel équipement par le ministère de la santé:

Le ministère sélectionne un code EMDN de la liste.

Il saisit la durée de vie (lifespan) de l'équipement.

Il planifie les dates de maintenance préventive pour l'équipement et éventuellement pour les pièces de rechange.

Il saisit la garantie (warranty).

Il clique sur "Créer", ce qui génère un code série (serialCode).

Le code série et les plans de maintenance préventive sont stockés dans la base de données.

Réception de l'équipement par un hôpital:

Le service de maintenance de l'hôpital crée un nouvel équipement dans l'inventaire.

Il saisit le code série (serialCode) déjà créé.

Il récupère les plans de maintenance préventive associés à l'équipement.

Si la marque (brand) n'existe pas encore dans la liste de l'hôpital, il la crée.

Il saisit le reste des informations (nom de l'équipement, marque, classe de risque, fournisseur, date d'acquisition, garantie, serviceId, hospitalId, montant, statut, etc.).

Si l'équipement a des pièces de rechange, il les crée également.

# Gestion des pièces de rechange:

Chaque pièce de rechange peut avoir ses propres plans de maintenance préventive.

Les pièces de rechange sont associées à un équipement spécifique via equipmentId.

# Améliorations apportées
 ##  Ajout de MaintenancePlan: 
J'ai ajouté une entité MaintenancePlan pour gérer les plans de maintenance préventive pour les équipements et les pièces de rechange.

 ## Ajout de hospitalId dans Brand: 
Pour permettre à chaque hôpital de gérer ses propres marques.

## Ajout de maintenancePlans dans Equipment et SparePart:
Pour stocker les plans de maintenance préventive associés à chaque équipement et pièce de rechange.




Explication de la logique et des relations entre les entités
Équipement (Equipment) : L'équipement contient des informations importantes comme le slaId qui le lie à un SLA spécifique. Le SLA détermine les conditions de maintenance pour cet équipement, y compris les délais de réponse et de résolution, ainsi que les pénalités en cas de non-respect des conditions.

Incident (Incident) : Un incident est signalé pour un équipement spécifique. Il a des informations comme equipmentId, reportedAt (la date de déclaration), et un penaltyApplied qui sera mis à jour si le SLA de l'équipement est violé. L'incident peut avoir un statut ("En attente", "En cours", "Résolu") et une description de la panne.

SLA (SLA) : Le SLA pour un équipement définit des conditions comme le temps maximal de réponse et de résolution, ainsi qu'une pénalité en cas de non-respect. Un SLA est lié à un maintenanceProviderId, qui est l'ID du prestataire de maintenance responsable de la résolution de l'incident.

Prestataire de maintenance (MaintenanceProvider) : Le prestataire de maintenance gère plusieurs SLA et est responsable de la résolution des incidents dans les délais définis par ces SLA.

Plan de maintenance (MaintenancePlan) : Cela fait partie de la gestion de la maintenance préventive. Chaque équipement peut avoir plusieurs plans de maintenance associés, qui sont utilisés pour s'assurer que l'équipement reste en bon état.

Problématique des pénalités et de la logique
Lorsque tu déclares un incident pour un équipement, la logique suivante pourrait être implémentée :

Lors de la déclaration de l'incident :

Un incident est créé pour un équipement spécifique.
Le système vérifie si un SLA est associé à cet équipement (equipment.slaId).
Si le SLA existe, le système doit vérifier les conditions de résolution. Cela inclut les délais de réponse et de résolution définis dans le SLA.
Calcul des pénalités :

Si l'incident dépasse le délai de résolution spécifié dans le SLA, la pénalité sera appliquée.
Cela pourrait se faire dans la méthode de résolution de l'incident, où tu calcules le temps écoulé entre la déclaration (reportedAt) et la résolution (resolvedAt).
Si ce temps dépasse le maxResolutionTime du SLA, une pénalité (penaltyAmount) est appliquée à l'incident.
Logique de contrôle pour les pénalités
Voici comment cela pourrait être mis en place dans ton service IncidentService :

java
Copier
Modifier
public Incident resolveIncident(String incidentId) {
// Récupérer l'incident par son ID
Incident incident = incidentRepository.findById(incidentId)
.orElseThrow(() -> new RuntimeException("Incident non trouvé"));

    incident.setResolvedAt(new Date());
    incident.setStatus("Résolu");

    // Récupérer l'équipement lié à l'incident
    Equipment equipment = equipmentRepository.findById(incident.getEquipmentId())
            .orElseThrow(() -> new RuntimeException("Équipement non trouvé"));

    // Vérifier si un SLA est défini pour cet équipement
    if (equipment.getSlaId() == null) {
        throw new RuntimeException("Aucun SLA défini pour cet équipement");
    }

    // Récupérer le SLA à partir de son ID
    SLA sla = slaRepository.findById(equipment.getSlaId())
            .orElseThrow(() -> new RuntimeException("SLA non trouvé pour cet équipement"));

    // Vérifier la durée de résolution et appliquer la pénalité si nécessaire
    long resolutionTime = (incident.getResolvedAt().getTime() - incident.getReportedAt().getTime()) / (1000 * 60 * 60); // en heures
    if (resolutionTime > sla.getMaxResolutionTime()) {
        // Appliquer la pénalité si le temps de résolution dépasse le délai du SLA
        incident.setPenaltyApplied(sla.getPenaltyAmount());
    }

    // Mettre à jour l'incident
    return incidentRepository.save(incident);
}
Processus détaillé :
Lors de la création d'un incident, tu vérifies si un SLA est associé à l'équipement (equipment.slaId).
Lors de la résolution de l'incident, tu compares le temps écoulé entre la déclaration et la résolution avec le maxResolutionTime du SLA.
Si le temps de résolution dépasse le délai autorisé, tu appliques la pénalité (penaltyAmount) spécifiée dans le SLA à l'incident.
L'incident est ensuite mis à jour avec la pénalité appliquée.
Compléments :
Lien avec le prestataire de maintenance : Si tu souhaites aussi savoir quel prestataire de maintenance est responsable de l'incident, tu peux facilement obtenir cette information à partir du SLA (sla.getMaintenanceProviderId()).

Gestion des incidents pour différents prestataires : Si tu as plusieurs prestataires de maintenance, chaque SLA est associé à un prestataire. Cela permet de savoir quel prestataire est responsable de quel SLA et, en conséquence, quel prestataire est responsable de la gestion des incidents.






ôles et relations
Un hôpital (Hospital) possède des équipements médicaux.
Chaque équipement est lié à un SLA (Service Level Agreement) qui définit les termes de la maintenance.
Un SLA est associé à une entreprise de maintenance (MaintenanceProvider).
Un MaintenanceProvider peut travailler avec plusieurs hôpitaux.
2️⃣ Exemple concret
Imaginons un scénario avec deux hôpitaux et une entreprise de maintenance :

🏥 Hôpitaux
Hôpital A (hospitalId: HOSP-001)
Hôpital B (hospitalId: HOSP-002)
🏢 Entreprise de maintenance
MaintenancePlus (maintenanceProviderId: MP-123)
Travaille avec les hôpitaux A et B (hospitalIds = ["HOSP-001", "HOSP-002"]).
A signé deux SLA :
SLA 1 pour un scanner de l’Hôpital A (slaId: SLA-001).
SLA 2 pour un IRM de l’Hôpital B (slaId: SLA-002).
3️⃣ Comment ça fonctionne dans la base de données ?
📌 Stockage des informations
Table MaintenanceProvider (Prestataire de maintenance)
maintenanceProviderId	name	hospitalIds	slaIds
MP-123	MaintenancePlus	["HOSP-001", "HOSP-002"]	["SLA-001", "SLA-002"]
Table SLA (Contrats de maintenance)
slaId	equipmentId	hospitalId	maintenanceProviderId
SLA-001	EQ-1001 (Scanner)	HOSP-001	MP-123
SLA-002	EQ-2001 (IRM)	HOSP-002	MP-123
Table Incident (Pannes signalées)
incidentId	equipmentId	hospitalId	slaId	maintenanceProviderId
INC-001	EQ-1001	HOSP-001	SLA-001	MP-123
INC-002	EQ-2001	HOSP-002	SLA-002	MP-123
4️⃣ Étapes du scénario en action
🛠️ Étape 1 : L’administrateur de l’hôpital crée un SLA
Hôpital A a un scanner (EQ-1001).
Il crée un SLA pour sa maintenance (SLA-001) et l’associe à MaintenancePlus (MP-123).
Hôpital B fait pareil pour son IRM (EQ-2001) avec un autre SLA (SLA-002) pour MaintenancePlus (MP-123).
➡ Résultat : MaintenancePlus gère la maintenance des deux hôpitaux.

📢 Étape 2 : Un technicien signale un incident
Hôpital A signale une panne sur le Scanner (EQ-1001).
Le système récupère son SLA (SLA-001) et voit que MaintenancePlus (MP-123) est responsable.
Un incident (INC-001) est créé et assigné à MaintenancePlus.
➡ Résultat : L’entreprise reçoit une notification pour résoudre l’incident.

🛠️ Étape 3 : Un employé de MaintenancePlus intervient
Un utilisateur avec le rôle ROLE_MAINTENANCE_COMPANY (ex: Jean Dupont) se connecte.
Il voit uniquement les incidents des hôpitaux avec lesquels MaintenancePlus travaille (HOSP-001 et HOSP-002).
Il résout l'incident et met à jour le statut.
➡ Résultat : L’incident est fermé ✅.

5️⃣ 🔥 Conclusion : Pourquoi hospitalIds et slaIds sont nécessaires ?
🚀 Problème	✅ Solution
Un MaintenanceProvider peut travailler avec plusieurs hôpitaux.	✅ hospitalIds permet de savoir avec quels hôpitaux un prestataire collabore.
Un MaintenanceProvider gère plusieurs SLA, mais chaque SLA appartient à un hôpital.	✅ slaIds permet de retrouver les contrats de maintenance gérés par un prestataire.
Un technicien doit voir uniquement les incidents des hôpitaux avec lesquels il travaille.	✅ En filtrant les incidents par hospitalId et maintenanceProviderId, on empêche un prestataire d’accéder aux incidents d’un autre hôpital.



# Relations entre les entités après correction :
Un MaintenanceProvider peut être associé à plusieurs hôpitaux (hospitalIds).
Un SLA est lié à un MaintenanceProvider et un hospitalId.
Un Equipment est lié à un hospitalId et un slaId.
Un User de type ROLE_MAINTENANCE_COMPANY est rattaché à un MaintenanceProvider via maintenanceProviderId.