package platformMedical.equipment_service.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Incident {

    @Id
    private String id;
    private String equipmentId; // ID de l'équipement concerné
    private String reportedBy; // ID de l'utilisateur qui signale la panne
    private LocalDateTime reportedAt; // Date de déclaration de la panne
    private LocalDateTime resolvedAt; // Date de résolution (null si non résolue)
    private String status; // Statut : "En attente", "En cours", "Résolu"
    private String description; // Description de la panne
    private double penaltyApplied; // Montant de la pénalité appliquée si SLA non respecté
    private String resolvedBy;
    private String resolutionDetails; // Détail de ce qui a été fait
    private String validatedBy; // Ingénieur ayant validé
    private String hospitalId;
    private LocalDateTime validatedAt;
    private String serviceId;
    private Severity severity;
    private boolean slaResponseViolated = false;
    private boolean slaResolutionViolated = false;



}

