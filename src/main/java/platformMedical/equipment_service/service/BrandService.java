package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.Brand;
import platformMedical.equipment_service.repository.BrandRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class BrandService {


    private final BrandRepository brandRepository;

    // Créer une nouvelle marque
    public Brand createBrand(Brand brand) {
        // Vérifier si la marque existe déjà pour cet hôpital
        Optional<Brand> existingBrand = brandRepository.findByNameAndHospitalId(brand.getName(), brand.getHospitalId());
        if (existingBrand.isPresent()) {
            throw new RuntimeException("Une marque avec ce nom existe déjà pour cet hôpital.");
        }

        // Enregistrer la marque dans la base de données
        return brandRepository.save(brand);
    }

    // Récupérer toutes les marques d'un hôpital
    public List<Brand> getBrandsByHospitalId(String hospitalId) {
        return brandRepository.findByHospitalId(hospitalId);
    }

    // Récupérer une marque par son ID
    public Brand getBrandById(String id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marque non trouvée"));
    }

    // Mettre à jour une marque
    public Brand updateBrand(String id, Brand updatedBrand) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marque non trouvée"));

        // Mettre à jour les champs modifiables
        if (updatedBrand.getName() != null) {
            brand.setName(updatedBrand.getName());
        }
        if (updatedBrand.getHospitalId() != null) {
            brand.setHospitalId(updatedBrand.getHospitalId());
        }

        return brandRepository.save(brand);
    }

    // Supprimer une marque
    public void deleteBrand(String id) {
        brandRepository.deleteById(id);
    }
}
