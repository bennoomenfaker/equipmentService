package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.SparePart;

import java.util.List;
import java.util.Optional;

@Repository
public interface SparePartRepository extends MongoRepository<SparePart, String> {


    // Trouver toutes les pièces de rechange d'un équipement spécifique
    List<SparePart> findByEquipmentId(String equipmentId);

    // Trouver toutes les pièces de rechange d'un hôpital spécifique
    List<SparePart> findByHospitalId(String hospitalId);


    void deleteByEquipmentId(String equipmentId);
}