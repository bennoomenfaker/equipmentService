package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Incident;

import java.util.List;
import java.util.Set;

@Repository
public interface IncidentRepository extends MongoRepository<Incident,String> {

  

    List<Incident> findAllByHospitalId(String hospitalId);

    List<Incident> findAllByHospitalIdAndServiceId(String hospitalId, String serviceId);

    List<Incident> findByStatusIn(List<String> strings);

    List<Incident> findByEquipmentIdIn(Set<String> equipmentIds);
}