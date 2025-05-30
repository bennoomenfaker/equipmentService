package platformMedical.equipment_service.controller;


import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.Supplier;
import platformMedical.equipment_service.service.SupplierService;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@AllArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    // Récupérer tous les fournisseurs d'un hôpital
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<Supplier>> getSuppliersByHospital(@PathVariable String hospitalId) {
        List<Supplier> suppliers = supplierService.findSupplierByHospitalId(hospitalId);
        return ResponseEntity.ok(suppliers);
    }

    // Récupérer un fournisseur par son id
    @GetMapping("/{id}")
    public ResponseEntity<Supplier> getSupplierById(@PathVariable String id) {
        return supplierService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Créer un nouveau fournisseur
    @PostMapping
    public ResponseEntity<Supplier> createSupplier(@RequestBody Supplier supplier) {
        Supplier savedSupplier = supplierService.updateSupplier(null, supplier); // Ou créer une méthode create si besoin
        return ResponseEntity.ok(savedSupplier);
    }

    // Mettre à jour un fournisseur existant
    @PutMapping("/{id}")
    public ResponseEntity<Supplier> updateSupplier(@PathVariable String id, @RequestBody Supplier updatedSupplier) {
        try {
            Supplier supplier = supplierService.updateSupplier(id, updatedSupplier);
            return ResponseEntity.ok(supplier);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Supprimer un fournisseur
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(@PathVariable String id) {
        try {
            supplierService.deleteSupplier(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
