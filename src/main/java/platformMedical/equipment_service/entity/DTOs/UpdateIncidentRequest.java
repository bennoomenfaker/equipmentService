package platformMedical.equipment_service.entity.DTOs;

import lombok.Data;

@Data
public class UpdateIncidentRequest {
    private IncidentDTO updatedData;
    private UserDTO user;
}

