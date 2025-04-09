package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.CorrectiveMaintenance;
import platformMedical.equipment_service.entity.DTOs.IncidentDTO;
import platformMedical.equipment_service.entity.DTOs.NotificationEvent;
import platformMedical.equipment_service.entity.DTOs.UserDTO;
import platformMedical.equipment_service.entity.DTOs.UserServiceClient;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.Incident;
import platformMedical.equipment_service.entity.SLA;
import platformMedical.equipment_service.kafka.KafkaProducerService;
import platformMedical.equipment_service.repository.CorrectiveMaintenanceRepository;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.IncidentRepository;
import platformMedical.equipment_service.repository.SLARepository;

import java.time.LocalDateTime;
import java.util.*;
@Slf4j
@Service
@AllArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final SLARepository slaRepository;
    private final EquipmentRepository equipmentRepository;
    private final CorrectiveMaintenanceRepository correctiveMaintenanceRepository;
    private final UserServiceClient userServiceClient;
    private final SlaService slaService;
    private final KafkaProducerService kafkaProducerService;

    public Incident reportIncident(String equipmentId, String description, String reportedByUserId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));

        Incident incident = new Incident();
        incident.setEquipmentId(equipment.getId());
        incident.setDescription(description);
        incident.setReportedBy(reportedByUserId);
        incident.setReportedAt(new Date());
        incident.setStatus("En attente");
        incident.setHospitalId(equipment.getHospitalId());
        incident.setServiceId(equipment.getServiceId());
        incident.setPenaltyApplied(0); // Par défaut, pas encore de pénalité

        incidentRepository.save(incident);

        // Récupération des utilisateurs concernés
        List<String> emailsToNotify = new ArrayList<>();

        // 1. Surveillants de service et ingénieurs de maintenance
        List<String> roles = Arrays.asList("ROLE_SERVICE_SUPERVISOR", "ROLE_MAINTENANCE_ENGINEER");
        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ",equipment.getHospitalId(), roles);
        users.forEach(user -> emailsToNotify.add(user.getEmail()));
        log.info(reportedByUserId);
        // 2. Utilisateur ayant signalé la panne
        UserDTO reportingUser = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ",reportedByUserId);
        log.info("report"+reportingUser);
        if (reportingUser != null && reportingUser.getEmail() != null) {
            emailsToNotify.add(reportingUser.getEmail());
        } else {
            log.error("Reporting user or email is null for ID: " + reportedByUserId);
            throw new IllegalArgumentException("Cannot find reporting user or email.");
        }
        // 3. Prestataire de maintenance (via SLA → userIdCompany)
        Optional<SLA> slaExist = slaService.getSlaById(equipment.getSlaId());
        if(slaExist.isPresent()){
            SLA sla = slaExist.get();
            UserDTO companyUser = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ",sla.getUserIdCompany());
            emailsToNotify.add(companyUser.getEmail());
        } else {
            log.warn("No SLA found for equipment ID: " + equipment.getId());
            // Optionnellement, vous pouvez renvoyer une exception ou gérer ce cas différemment.
        }



        // Création de l’événement Kafka
        NotificationEvent notificationEvent = new NotificationEvent(
                "Nouvel incident signalé",
                "Un incident a été signalé sur l’équipement : " + equipment.getNom(),
                emailsToNotify
        );

        kafkaProducerService.sendMessage("notification-events", notificationEvent);

        return incident;
    }

    public Incident resolveIncident(String incidentId, String resolutionDescription, String engineerId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

        incident.setResolvedBy(engineerId);
        incident.setResolvedAt(new Date());
        incident.setResolutionDetails(resolutionDescription);
        incident.setStatus("Résolu");
        incidentRepository.save(incident);

        // Récupérer l’équipement concerné
        Equipment equipment = equipmentRepository.findById(incident.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));

        // Récupérer le SLA pour identifier la société de maintenance
        SLA sla = slaService.getSlaById(equipment.getSlaId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));

        // Création de la maintenance corrective
        CorrectiveMaintenance maintenance = CorrectiveMaintenance.builder()
                .equipmentId(equipment.getId())
                .incidentId(incident.getId())
                .assignedTo(sla.getUserIdCompany()) // prestataire responsable
                .description(resolutionDescription)
                .plannedDate(incident.getReportedAt())
                .completedDate(new Date()) // maintenant car on résout
                .status("Terminée")
                .build();

        correctiveMaintenanceRepository.save(maintenance);

        // Notification
        List<String> emailsToNotify = new ArrayList<>();

        // 1. Surveillants de service et ingénieurs de maintenance
        List<String> roles = Arrays.asList("ROLE_SERVICE_SUPERVISOR", "ROLE_MAINTENANCE_ENGINEER");
        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ", // token
                equipment.getHospitalId(), roles);
        users.forEach(user -> emailsToNotify.add(user.getEmail()));

        // 2. Ingénieur qui a résolu
        UserDTO resolvingEngineer = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ", engineerId);
        emailsToNotify.add(resolvingEngineer.getEmail());

        // 3. Société de maintenance
        UserDTO companyUser = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ", sla.getUserIdCompany());
        emailsToNotify.add(companyUser.getEmail());

        // Envoi via Kafka
        NotificationEvent notificationEvent = new NotificationEvent(
                "Incident résolu",
                "L’incident sur l’équipement " + equipment.getNom() +
                        " a été résolu par " + resolvingEngineer.getFirstName() + " " + resolvingEngineer.getLastName(),
                emailsToNotify
        );

        kafkaProducerService.sendMessage("notification-events", notificationEvent);

        return incident;
    }

    // Méthodes pour consulter les incidents
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    public List<Incident> getIncidentsByHospitalId(String hospitalId) {
        return incidentRepository.findAllByHospitalId(hospitalId);
    }

    public List<Incident> getIncidentsByHospitalIdAndServiceId(String hospitalId, String serviceId) {
        return incidentRepository.findAllByHospitalIdAndServiceId(hospitalId, serviceId);
    }


    public Incident updateIncident(String incidentId, Incident updatedIncidentData) {
        Incident existingIncident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé avec l'ID : " + incidentId));

        // Met à jour uniquement les champs fournis (s'ils ne sont pas null)
        if (updatedIncidentData.getDescription() != null) {
            existingIncident.setDescription(updatedIncidentData.getDescription());
        }

        if (updatedIncidentData.getStatus() != null) {
            existingIncident.setStatus(updatedIncidentData.getStatus());
        }

        if (updatedIncidentData.getResolvedBy() != null) {
            existingIncident.setResolvedBy(updatedIncidentData.getResolvedBy());
        }

        if (updatedIncidentData.getResolutionDetails() != null) {
            existingIncident.setResolutionDetails(updatedIncidentData.getResolutionDetails());
        }

        if (updatedIncidentData.getValidatedBy() != null) {
            existingIncident.setValidatedBy(updatedIncidentData.getValidatedBy());
        }

        if (updatedIncidentData.getResolvedAt() != null) {
            existingIncident.setResolvedAt(updatedIncidentData.getResolvedAt());
        }

        if (updatedIncidentData.getPenaltyApplied() != 0) {
            existingIncident.setPenaltyApplied(updatedIncidentData.getPenaltyApplied());
        }

        // On ne permet généralement pas de modifier les IDs liés à l'équipement, hôpital ou l'auteur du rapport

        return incidentRepository.save(existingIncident);
    }

}
