package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class IncidentStatusUpdateEvent {
    private String incidentId;
    private String serialCode;
    private String equipmentName;
    private String oldStatus;
    private String newStatus;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private List<String> emails;

}

