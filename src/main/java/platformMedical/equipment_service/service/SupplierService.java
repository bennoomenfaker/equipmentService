package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.Supplier;
import platformMedical.equipment_service.repository.SupplierRepository;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;


    // Trouver les fournisseurs par hospitalId
    public List<Supplier> findSupplierByHospitalId(String hospitalId) {
        return supplierRepository.findAllByHospitalIdAndActivatedTrue(hospitalId);
    }

    // Trouver un fournisseur par son id
    public Optional<Supplier> findById(String id) {
        return supplierRepository.findById(id);
    }

    // Mettre à jour un fournisseur existant
    public Supplier updateSupplier(String id, Supplier updatedSupplier) {
        return supplierRepository.findById(id)
                .map(supplier -> {
                    supplier.setName(updatedSupplier.getName());
                    // Mise à jour des nouveaux champs email et tel
                    supplier.setEmail(updatedSupplier.getEmail());
                    supplier.setTel(updatedSupplier.getTel());
                    return supplierRepository.save(supplier);
                })
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id " + id));
    }


    // Supprimer un fournisseur par son id
    public void deleteSupplier(String id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Fournisseur non trouvé avec l'id " + id));

        supplier.setActivated(false);
        supplierRepository.save(supplier);
    }

}
