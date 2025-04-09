package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.DTOs.IncidentDTO;
import platformMedical.equipment_service.entity.Incident;
import platformMedical.equipment_service.service.IncidentService;

import java.util.List;

@RestController
@RequestMapping("/api/incidents")
@AllArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    /**
     * Signale un nouvel incident pour un équipement donné.
     *
     * @param equipmentId  ID de l'équipement concerné
     * @param reportedBy   Utilisateur ayant signalé l'incident
     * @param description  Description détaillée du problème
     * @return L'incident créé
     */
    @PostMapping("/report")
    public ResponseEntity<Incident> reportIncident(
            @RequestParam String equipmentId,
            @RequestParam String description ,
            @RequestParam String  reportedBy) {
        Incident incident = incidentService.reportIncident(equipmentId,description ,reportedBy );
        return ResponseEntity.ok(incident);
    }

    /**
     * Marque un incident comme résolu et applique une éventuelle pénalité
     * si le temps de résolution dépasse le SLA.
     *
     * @param incidentId   ID de l'incident à résoudre
     * @param resolveRequest Objet contenant l'ID de l'ingénieur et les détails de la résolution
     * @return L'incident mis à jour avec les informations de résolution
     */
    @PutMapping("/resolve/{incidentId}")
    public ResponseEntity<Incident> resolveIncident(@PathVariable String incidentId,
                                                    @RequestBody Incident resolveRequest) {
        // Utiliser les champs nécessaires de l'objet resolveRequest (validatedBy, resolutionDetails)
        Incident incident = incidentService.resolveIncident(incidentId, resolveRequest.getValidatedBy(), resolveRequest.getResolutionDetails());
        return ResponseEntity.ok(incident);
    }

    /**
     * Récupère tous les incidents associés à tous les hôpitaux.
     *
     * @return Liste des incidents avec les détails des équipements
     */
    @GetMapping("/all")
    public ResponseEntity<List<Incident>> getAllIncidents() {
        List<Incident> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Récupère les incidents par hospitalId.
     *
     * @param hospitalId L'ID de l'hôpital
     * @return Liste des incidents associés à cet hôpital
     */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<Incident>> getIncidentsByHospitalId(@PathVariable String hospitalId) {
        List<Incident> incidents = incidentService.getIncidentsByHospitalId(hospitalId);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Récupère les incidents par hospitalId et serviceId.
     *
     * @param hospitalId L'ID de l'hôpital
     * @param serviceId  L'ID du service
     * @return Liste des incidents associés à cet hôpital et service
     */
    @GetMapping("/hospital/{hospitalId}/service/{serviceId}")
    public ResponseEntity<List<Incident>> getIncidentsByHospitalIdAndServiceId(@PathVariable String hospitalId,
                                                                                  @PathVariable String serviceId) {
        List<Incident> incidents = incidentService.getIncidentsByHospitalIdAndServiceId(hospitalId, serviceId);
        return ResponseEntity.ok(incidents);
    }

    /**
     * Met à jour un incident existant.
     *
     * @param incidentId L'ID de l'incident à mettre à jour
     * @param updateRequest L'objet Incident contenant les champs à mettre à jour
     * @return L'incident mis à jour
     */
    @PutMapping("/update/{incidentId}")
    public ResponseEntity<Incident> updateIncident(@PathVariable String incidentId,
                                                   @RequestBody Incident updateRequest) {
        Incident updatedIncident = incidentService.updateIncident(incidentId, updateRequest);
        return ResponseEntity.ok(updatedIncident);
    }


}
