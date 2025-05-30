package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Supplier;

import java.util.List;

@Repository
public interface SupplierRepository extends MongoRepository<Supplier , String> {
    List<Supplier> findAllByHospitalIdAndActivatedTrue(String hospitalId);

}
