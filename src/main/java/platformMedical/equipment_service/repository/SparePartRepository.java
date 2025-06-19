package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.entity.SparePart;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparePartRepository extends MongoRepository<SparePart, String> {






    List<SparePart> findByHospitalId(String hospitalId);

    List<SparePart> findByEmdnCode(String emdnCode);

    List<SparePart> findByHospitalIdAndEmdnCode(String hospitalId, String emdnCode);
}