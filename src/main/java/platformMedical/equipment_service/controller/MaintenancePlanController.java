package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.DTOs.MessageResponse;
import platformMedical.equipment_service.entity.MaintenancePlan;
import platformMedical.equipment_service.service.MaintenancePlanService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/maintenance-plans")
@AllArgsConstructor
public class MaintenancePlanController {

    private final MaintenancePlanService maintenancePlanService;

    // Créer un plan de maintenance pour un équipement
    @PostMapping("/{equipmentId}")
    public ResponseEntity<MessageResponse> createMaintenancePlanForEquipment(
            @PathVariable String equipmentId,
            @RequestBody MaintenancePlan maintenancePlan) {
        try {
            MessageResponse response = maintenancePlanService.createMaintenancePlan(equipmentId, maintenancePlan);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new MessageResponse("Erreur lors de la création du plan de maintenance : " + e.getMessage()));
        }
    }

    // Récupérer un plan de maintenance par son ID
    @GetMapping("/{equipmentId}/{maintenancePlanId}")
    public ResponseEntity<MaintenancePlan> getMaintenancePlan(
            @PathVariable String equipmentId,
            @PathVariable String maintenancePlanId) {

        try {
            // Appeler le service pour obtenir le plan de maintenance
            MaintenancePlan plan = maintenancePlanService.getMaintenancePlanByIdAndEquipmentId(equipmentId, maintenancePlanId);

            // Retourner le plan de maintenance avec un statut 200 OK
            return ResponseEntity.ok(plan);
        } catch (Exception e) {
            // Si une exception est levée, on renvoie une erreur 404
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    // Mettre à jour un plan de maintenance
    @PutMapping("/{equipmentId}/{maintenancePlanId}")
    public ResponseEntity<MessageResponse> updateMaintenancePlan(
            @PathVariable String equipmentId,
            @PathVariable String maintenancePlanId,
            @RequestBody MaintenancePlan newPlanDetails) {
        // Appeler le service pour mettre à jour le plan de maintenance
        MessageResponse response = maintenancePlanService.updateMaintenancePlan(equipmentId, maintenancePlanId, newPlanDetails);

        // Si le message contient "avec succès", on renvoie un statut OK
        if (response.getMessage().contains("avec succès")) {
            return ResponseEntity.ok(response);
        } else {
            // Sinon, on renvoie une erreur
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }






    @GetMapping("/hospital/{hospitalId}/getAllMaintenance")
    public ResponseEntity<List<MaintenancePlan>> getAllMaintenancePlansByHospital(
            @PathVariable String hospitalId,
            @RequestHeader("Authorization") String token) {
        log.info("hospital" , hospitalId);
        System.out.println(hospitalId);

        List<MaintenancePlan> maintenancePlans = maintenancePlanService.getAllMaintenancePlansByHospital(hospitalId, token);
        log.info("res"  , maintenancePlans);
        System.out.println(maintenancePlans+"ddd");
        return ResponseEntity.ok(maintenancePlans);
    }


    @PostMapping("/trigger/equipments")
    public ResponseEntity<?> triggerEquipmentMaintenanceCheck(@RequestHeader("Authorization") String token) {
        try {
            maintenancePlanService.trackMaintenanceForEquipment(); // Appelle la méthode de service existante
            return ResponseEntity.ok("Vérification manuelle de la maintenance des équipements déclenchée.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erreur lors du déclenchement manuel de la vérification de la maintenance des équipements : " + e.getMessage());
        }
    }


    @DeleteMapping("/{maintenancePlanId}")
    public void deleteMaintenancePlan(@PathVariable String maintenancePlanId) {
        maintenancePlanService.deleteMaintenancePlanId(maintenancePlanId);
    }
}

