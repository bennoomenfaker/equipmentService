package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.Incident;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IncidentWithEquipmentDTO {
    private Incident incident;
    private Equipment equipment;
    private UserDTO userDTO;
    private HospitalServiceEntity hospitalServiceEntity;

    // 1. Incident + Equipment uniquement
    public IncidentWithEquipmentDTO(Incident incident, Equipment equipment) {
        this.incident = incident;
        this.equipment = equipment;
    }

    // 2. Incident + Equipment + User uniquement
    public IncidentWithEquipmentDTO(Incident incident, Equipment equipment, UserDTO userDTO) {
        this.incident = incident;
        this.equipment = equipment;
        this.userDTO = userDTO;
    }

    // 3. Incident + Equipment + Service uniquement
    public IncidentWithEquipmentDTO(Incident incident, Equipment equipment, HospitalServiceEntity hospitalServiceEntity) {
        this.incident = incident;
        this.equipment = equipment;
        this.hospitalServiceEntity = hospitalServiceEntity;
    }
}
