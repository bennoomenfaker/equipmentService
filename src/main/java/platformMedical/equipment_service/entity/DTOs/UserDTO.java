package platformMedical.equipment_service.entity.DTOs;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class UserDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String telephone;
    private String hospitalId;
    private String serviceId;
    private RoleDTO role;

}

@Getter @Setter
 class RoleDTO {
    private String id;
    private String name;

}