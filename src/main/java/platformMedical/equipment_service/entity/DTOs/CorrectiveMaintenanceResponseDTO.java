package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.Incident;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CorrectiveMaintenanceResponseDTO {
    private String id;
    private String description;
    private String status;
    private LocalDateTime plannedDate;    // correspond à la date de début prévue
    private LocalDateTime completedDate;  // correspond à la date de fin réelle
    private String resolutionDetails;


    private Equipment equipment;
    private Incident incident;

    private UserDTO assignedTo;
    private UserDTO validatedBy;
    private UserDTO resolvedBy;
    private HospitalServiceEntity hospitalServiceEntity;
}
