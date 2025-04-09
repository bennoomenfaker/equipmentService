package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Incident;

import java.util.List;

@Repository
public interface IncidentRepository extends MongoRepository<Incident,String> {

  

    List<Incident> findAllByHospitalId(String hospitalId);

    List<Incident> findAllByHospitalIdAndServiceId(String hospitalId, String serviceId);
}