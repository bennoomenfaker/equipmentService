package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SupervisorInfo {
    private String firstName;
    private String lastName;
    private String email;
    private String serviceId;

}
