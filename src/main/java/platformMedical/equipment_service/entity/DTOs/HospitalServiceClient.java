package platformMedical.equipment_service.entity.DTOs;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;


@FeignClient(name = "hospital-service", url = "http://localhost:9999/hospital-service/api")
public interface HospitalServiceClient {

    @GetMapping("/hospitals/{hospitalId}/name")
    String getHospitalNameById(
            @RequestHeader("Authorization") String token,
            @PathVariable("hospitalId") String hospitalId
    );

    @GetMapping("/services/{id}")
    ResponseEntity<HospitalServiceEntity> getServiceById(
            @RequestHeader("Authorization") String token,
            @PathVariable("id") String serviceId
    );
}


