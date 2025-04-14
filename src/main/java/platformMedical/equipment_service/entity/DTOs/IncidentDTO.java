package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDTO {
    private String severity;
    private String status;
    private String description;
    private String resolutionDetails;
    private String validatedBy;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    private double penaltyApplied; // Montant de la pénalité appliquée si SLA non respecté

    // autres champs et getters/setters
}
