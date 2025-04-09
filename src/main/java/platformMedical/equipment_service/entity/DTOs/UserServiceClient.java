package platformMedical.equipment_service.entity.DTOs;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "user-service", url = "http://localhost:9999/auth-user-service/api/users")
public interface UserServiceClient {

    @GetMapping("/hospital/{hospitalId}/roles")
    List<UserDTO> getUsersByHospitalAndRoles(
            @RequestHeader("Authorization") String token,
            @PathVariable String hospitalId,
            @RequestParam List<String> roles
    );

    // Récupérer l'admin d'un hôpital en utilisant l'hospitalId
    @GetMapping("/hospital/{hospitalId}/admin")
    UserDTO getAdminByHospitalId(
            @RequestHeader("Authorization") String token,
            @PathVariable String hospitalId
    );

    // Récupérer un utilisateur par son userId
    @GetMapping("/{userId}")
    UserDTO getUserById(
            @RequestHeader("Authorization") String token,
            @PathVariable String userId
    );

    @GetMapping("/supervisors/{serviceId}")
    List<UserDTO> getServiceSupervisors(
            @RequestHeader("Authorization") String token,
            @PathVariable String serviceId
    );

}
