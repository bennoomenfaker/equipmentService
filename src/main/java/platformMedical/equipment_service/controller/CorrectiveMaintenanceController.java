package platformMedical.equipment_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.CorrectiveMaintenance;
import platformMedical.equipment_service.entity.DTOs.CorrectiveMaintenanceResponseDTO;
import platformMedical.equipment_service.service.CorrectiveMaintenanceService;

import java.util.List;

@RestController
@RequestMapping("/api/corrective-maintenances")
@RequiredArgsConstructor
public class CorrectiveMaintenanceController {

    private final CorrectiveMaintenanceService correctiveMaintenanceService;

    //  1. Récupérer toutes les maintenances correctives
    @GetMapping
    public ResponseEntity<List<CorrectiveMaintenanceResponseDTO>> getAll() {
        return ResponseEntity.ok(correctiveMaintenanceService.getAllCorrectiveMaintenances());
    }

    //  2. Récupérer les maintenances correctives assignées à une société de maintenance
    @GetMapping("/company/{userIdCompany}")
    public ResponseEntity<List<CorrectiveMaintenanceResponseDTO>> getByCompany(@PathVariable String userIdCompany) {
        return ResponseEntity.ok(correctiveMaintenanceService.getCorrectiveMaintenancesByCompany(userIdCompany));
    }

    //  3. Mettre à jour une maintenance corrective
    @PutMapping("/{id}")
    public ResponseEntity<CorrectiveMaintenance> update(@PathVariable String id,
                                                        @RequestBody CorrectiveMaintenance updated) {
        CorrectiveMaintenance updatedMaintenance = correctiveMaintenanceService.updateCorrectiveMaintenance(id, updated);
        return ResponseEntity.ok(updatedMaintenance);
    }

    //  4. Supprimer une maintenance corrective
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        correctiveMaintenanceService.deleteCorrectiveMaintenance(id);
        return ResponseEntity.noContent().build();
    }
}

