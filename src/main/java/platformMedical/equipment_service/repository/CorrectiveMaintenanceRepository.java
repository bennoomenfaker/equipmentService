package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.CorrectiveMaintenance;

import java.util.List;

@Repository
public interface CorrectiveMaintenanceRepository  extends MongoRepository<CorrectiveMaintenance , String> {
    List<CorrectiveMaintenance> findByAssignedTo(String userIdCompany);
}
