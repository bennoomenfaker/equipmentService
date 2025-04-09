package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.DTOs.EquipmentRequest;
import platformMedical.equipment_service.entity.DTOs.MessageResponse;
import platformMedical.equipment_service.entity.DTOs.UserDTO;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.MaintenancePlan;
import platformMedical.equipment_service.entity.SparePart;
import platformMedical.equipment_service.service.EquipmentService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/equipments")
@AllArgsConstructor
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
        public MessageResponse createEquipment(@RequestBody EquipmentRequest equipment) {
        return equipmentService.createEquipment(equipment);
    }

    @PostMapping("/{equipmentId}/maintenance-plans")
    public Equipment addMaintenancePlan(@PathVariable String equipmentId, @RequestBody MaintenancePlan maintenancePlan) {
        return equipmentService.addMaintenancePlan(equipmentId, maintenancePlan);
    }

    @GetMapping("/hospital/{hospitalId}")
    public List<Equipment> getEquipmentByHospitalId(@PathVariable String hospitalId) {
        return equipmentService.getEquipmentByHospitalId(hospitalId);
    }

    @GetMapping("/{equipmentId}/spare-parts")
    public List<SparePart> getSparePartsByEquipmentId(@PathVariable String equipmentId) {
        return equipmentService.getSparePartsByEquipmentId(equipmentId);
    }

    @PostMapping("/{equipmentId}/spare-parts")
    public Equipment addSparePart(@PathVariable String equipmentId, @RequestBody SparePart sparePart) {
        return equipmentService.addSparePart(equipmentId, sparePart);
    }

    @PutMapping("/{equipmentId}")
    public Equipment updateEquipment(@PathVariable String equipmentId, @RequestBody EquipmentRequest updatedEquipment) {
        return equipmentService.updateEquipment(equipmentId, updatedEquipment);
    }


    @GetMapping("/{code}")
    public ResponseEntity<EmdnNomenclature> getEmdnByCode(@PathVariable String code) {
        Optional<EmdnNomenclature> emdnNomenclature = equipmentService.findByCodeRecursive(code);
        return emdnNomenclature
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());

    }
    @GetMapping("/serial/{serialCode}")
    public ResponseEntity<Equipment> getEquipmentBySerialNumber(@PathVariable String serialCode) {
        return equipmentService.findBySerialNumber(serialCode)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{equipmentId}/assign-sla/{slaId}")
    public ResponseEntity<Equipment> assignSlaToEquipment(@PathVariable String equipmentId, @PathVariable String slaId) {
        return ResponseEntity.ok(equipmentService.assignSlaToEquipment(equipmentId, slaId));
    }

    @PutMapping("/reception/{serialNumber}")
    public ResponseEntity<MessageResponse> updateEquipmentAfterReception(
            @PathVariable String serialNumber,
            @RequestBody EquipmentRequest request) {

        MessageResponse updatedEquipment = equipmentService.updateEquipmentAfterReception(serialNumber, request);
        return ResponseEntity.ok(updatedEquipment);
    }

    @GetMapping("/non-received")
    public List<Equipment> getAllNonReceivedEquipment() {
        return equipmentService.getAllNonReceivedEquipment();
    }


    /**
     * Mise à jour des plans de maintenance d'un équipement
     * @param equipmentId Identifiant de l'équipement
     * @param updatedPlans Liste des plans de maintenance mis à jour
     * @return MessageResponse indiquant le succès ou l'échec de l'opération
     */
    @PutMapping("/{equipmentId}/maintenance-plans")
    public ResponseEntity<MessageResponse> updateMaintenancePlans(
            @PathVariable String equipmentId,
            @RequestBody List<MaintenancePlan> updatedPlans) {

        MessageResponse response = equipmentService.updateMaintenancePlanForEquipment(equipmentId, updatedPlans);

        if (response.getMessage().contains("Erreur")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        return ResponseEntity.ok(response);
    }



    /**
     * Endpoint pour changer un équipement entre services
     * @param equipmentId L'ID de l'équipement
     * @param newServiceId Le nouvel ID du service
     * @param description La description du changement
     * @param user L'utilisateur qui effectue l'opération
     * @return L'équipement mis à jour
     */
    @PutMapping("/{equipmentId}/service")
    public ResponseEntity<Equipment> changeEquipmentInterService(
            @PathVariable String equipmentId,
            @RequestParam String newServiceId,
            @RequestParam String description,
            @RequestBody UserDTO user,
            @RequestHeader("Authorization") String token) {

        Equipment updatedEquipment = equipmentService.changeEquipmentInterService(
                equipmentId, newServiceId, description, user,token);

        return ResponseEntity.ok(updatedEquipment);
    }

    /**
     * Endpoint pour changer un équipement entre hôpitaux
     * @param equipmentId L'ID de l'équipement
     * @param newHospitalId Le nouvel ID de l'hôpital
     * @param description La description du changement
     * @param user L'utilisateur qui effectue l'opération
     * @return L'équipement mis à jour
     */
    @PutMapping("/{equipmentId}/hospital")
    public ResponseEntity<Equipment> changeEquipmentInterHospital(
            @PathVariable String equipmentId,
            @RequestParam String newHospitalId,
            @RequestParam String description,
            @RequestBody UserDTO user,
            @RequestHeader("Authorization") String token) {

        Equipment updatedEquipment = equipmentService.changeEquipmentInterHospital(
                equipmentId, newHospitalId, description, user , token);

        return ResponseEntity.ok(updatedEquipment);
    }


    // Supprimer un équipement avec ses dépendances
    @DeleteMapping("/{equipmentId}")
    public ResponseEntity<MessageResponse> deleteEquipment(@PathVariable String equipmentId) {
        try {
            equipmentService.deleteEquipment(equipmentId);
            return ResponseEntity.ok(new MessageResponse("Équipement supprimé avec succès"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MessageResponse("Erreur : " + e.getMessage()));
        }
    }

    @GetMapping("/by-id/{id}")
    public ResponseEntity<Equipment> getEquipmentById(@PathVariable String id) {
        return equipmentService.findEquipmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }




}