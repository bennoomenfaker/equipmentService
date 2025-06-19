package platformMedical.equipment_service.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.Alert;
import platformMedical.equipment_service.service.AlertService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    // 🔹 Récupérer toutes les alertes d'un hôpital via son ID
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<Alert>> getAlertsByHospitalId(@PathVariable String hospitalId) {
        List<Alert> alerts = alertService.getAlertsByHospitalId(hospitalId);
        log.info(hospitalId);
        log.info(alerts.toString());
        return ResponseEntity.ok(alerts);
    }

    // 🔹 Récupérer toutes les alertes d’un équipement donné
    @GetMapping("/equipment/{equipmentId}")
    public ResponseEntity<List<Alert>> getAlertsByEquipmentId(@PathVariable String equipmentId) {
        List<Alert> alerts = alertService.getAlertsByEquipmentId(equipmentId);
        return ResponseEntity.ok(alerts);
    }
}
