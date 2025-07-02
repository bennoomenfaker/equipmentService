package platformMedical.equipment_service.entity.DTOs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class SlaComplianceStats {
    private long totalIncidents;
    private long slaRespected;
    private long responseViolated;
    private long resolutionViolated;
    private double complianceRate;
}
