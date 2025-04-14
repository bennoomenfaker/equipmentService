package platformMedical.equipment_service.controller;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.DTOs.IncidentDTO;
import platformMedical.equipment_service.entity.DTOs.IncidentWithEquipmentDTO;
import platformMedical.equipment_service.entity.DTOs.UpdateIncidentRequest;
import platformMedical.equipment_service.entity.DTOs.UserDTO;
import platformMedical.equipment_service.entity.Incident;
import platformMedical.equipment_service.entity.Severity;
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
    public ResponseEntity<List<IncidentWithEquipmentDTO>> getAllIncidents() {
        List<IncidentWithEquipmentDTO> incidents = incidentService.getAllIncidents();
        return ResponseEntity.ok(incidents);
    }

    /**
     * Récupère les incidents par hospitalId.
     *
     * @param hospitalId L'ID de l'hôpital
     * @return Liste des incidents associés à cet hôpital
     */
    @GetMapping("/hospital/{hospitalId}")
    public ResponseEntity<List<IncidentWithEquipmentDTO>> getIncidentsByHospitalId(@PathVariable String hospitalId) {
        List<IncidentWithEquipmentDTO> incidents = incidentService.getIncidentsByHospitalId(hospitalId);
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
    public ResponseEntity<List<IncidentWithEquipmentDTO>> getIncidentsByHospitalIdAndServiceId(@PathVariable String hospitalId,
                                                                                  @PathVariable String serviceId) {
        List<IncidentWithEquipmentDTO> incidents = incidentService.getIncidentsByHospitalIdAndServiceId(hospitalId, serviceId);
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
                                                   @RequestBody UpdateIncidentRequest updateRequest) {
        Incident updatedIncident = incidentService.updateIncident(incidentId, updateRequest.getUpdatedData(), updateRequest.getUser());
        return ResponseEntity.ok(updatedIncident);
    }


    @PutMapping("/validate/{incidentId}")
    public ResponseEntity<Incident> validateIncident(
            @PathVariable String incidentId,
            @RequestParam String engineerId,
            @RequestParam String severity,
            @RequestBody IncidentDTO updatedIncidentData) {

        Incident validatedIncident;

            // Appel au service pour valider et potentiellement mettre à jour d'autres données
            validatedIncident = incidentService.validateIncident(incidentId, engineerId,severity, updatedIncidentData);


        return ResponseEntity.ok(validatedIncident);
    }


    /**
     * Récupère les incidents par hospitalId.
     *
     * @param incidentId L'ID de l'incident

     **/
    @DeleteMapping("/delete/{incidentId}")
    public ResponseEntity<String> deleteIncidentById(@PathVariable String incidentId) {
        try {
            incidentService.deleteIncidentById(incidentId); // méthode à implémenter dans le service
            return ResponseEntity.ok("Incident supprimé avec succès");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erreur lors de la suppression de l'incident : " + e.getMessage());
        }
    }

}
