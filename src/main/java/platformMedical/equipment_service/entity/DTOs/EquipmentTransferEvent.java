package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EquipmentTransferEvent {
    private String serialCode;
    private String equipmentId;
    private String name;
    private String description;
    private String oldHospitalId;
    private String oldHospitalName;
    private String newHospitalId;
    private String newHospitalName;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> emailsToNotify;

}