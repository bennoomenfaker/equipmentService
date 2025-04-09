package platformMedical.equipment_service.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import platformMedical.equipment_service.entity.Brand;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends MongoRepository<Brand, String> {

    // Trouver une marque par son nom
    Optional<Brand> findByName(String name);

    // Trouver toutes les marques d'un hôpital spécifique
    List<Brand> findByHospitalId(String hospitalId);

    Optional<Brand> findByNameAndHospitalId(String name, String hospitalId);

}
