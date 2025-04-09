package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentDTO {
    private String incidentId;
    private String equipmentId;
    private String reportedBy;
    private Date reportedAt;
    private String status;
    private String description;
    private String hospitalId;
    private String serviceId;
}
