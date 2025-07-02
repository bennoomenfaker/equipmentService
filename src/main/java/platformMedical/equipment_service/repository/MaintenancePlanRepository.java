package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.MaintenancePlan;

import java.util.Date;
import java.util.List;

@Repository
public interface MaintenancePlanRepository extends MongoRepository<MaintenancePlan, String> {




    void deleteByEquipmentId(String equipmentId);

}
