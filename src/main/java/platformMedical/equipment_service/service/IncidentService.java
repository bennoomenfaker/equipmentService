package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import platformMedical.equipment_service.entity.*;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.kafka.KafkaProducerService;
import platformMedical.equipment_service.repository.CorrectiveMaintenanceRepository;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.IncidentRepository;
import platformMedical.equipment_service.repository.SLARepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final SLARepository slaRepository;
    private final EquipmentRepository equipmentRepository;
    private final CorrectiveMaintenanceRepository correctiveMaintenanceRepository;
    private final UserServiceClient userServiceClient;
    private final HospitalServiceClient hospitalServiceClient;
    private final SlaService slaService;
    private final KafkaProducerService kafkaProducerService;
    private final String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ";

    public Incident reportIncident(String equipmentId, String description, String reportedByUserId) {
        Equipment equipment = equipmentRepository.findById(equipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));

        Incident incident = new Incident();
        incident.setEquipmentId(equipment.getId());
        incident.setDescription(description);
        incident.setReportedBy(reportedByUserId);
        incident.setReportedAt(LocalDateTime.now());
        incident.setStatus("En attente");
        incident.setHospitalId(equipment.getHospitalId());
        incident.setServiceId(equipment.getServiceId());
        incident.setPenaltyApplied(0); // Par défaut, pas encore de pénalité
        equipment.setStatus("en panne");

        incidentRepository.save(incident);
        equipmentRepository.save(equipment);

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
        incident.setResolvedAt(LocalDateTime.now());
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
                .completedDate(LocalDateTime.now()) // maintenant car on résout
                .status("Terminée")
                .resolutionDetails("")
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
    public List<IncidentWithEquipmentDTO> getAllIncidents() {
        List<Incident> incidents = incidentRepository.findAll();

        return incidents.stream()
                .map(incident -> {
                    // Récupération des données optionnelles
                    Equipment equipment = incident.getEquipmentId() != null
                            ? equipmentRepository.findById(incident.getEquipmentId()).orElse(null)
                            : null;

                    UserDTO userDTO = null;
                    try {
                        userDTO = incident.getReportedBy() != null
                                ? userServiceClient.getUserById(token, incident.getReportedBy())
                                : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'utilisateur pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    HospitalServiceEntity service = null;
                    try {
                        ResponseEntity<HospitalServiceEntity> serviceResponse = incident.getServiceId() != null
                                ? hospitalServiceClient.getServiceById(token, incident.getServiceId())
                                : null;
                        service = serviceResponse != null ? serviceResponse.getBody() : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération du service pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    // Construction du DTO selon les données disponibles
                    if (userDTO != null && equipment != null && service != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO, service);
                    } else if (equipment != null && userDTO != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO);
                    } else  {
                        return new IncidentWithEquipmentDTO(incident, equipment);
                    }
                })
                .collect(Collectors.toList());
    }

    public List<IncidentWithEquipmentDTO> getIncidentsByHospitalId(String hospitalId) {
        return incidentRepository.findAllByHospitalId(hospitalId).stream()
                .map(incident -> {
                    Equipment equipment = incident.getEquipmentId() != null
                            ? equipmentRepository.findById(incident.getEquipmentId()).orElse(null)
                            : null;

                    UserDTO userDTO = null;
                    try {
                        userDTO = incident.getReportedBy() != null
                                ? userServiceClient.getUserById(token, incident.getReportedBy())
                                : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'utilisateur pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    HospitalServiceEntity service = null;
                    try {
                        ResponseEntity<HospitalServiceEntity> serviceResponse = incident.getServiceId() != null
                                ? hospitalServiceClient.getServiceById(token, incident.getServiceId())
                                : null;
                        service = serviceResponse != null ? serviceResponse.getBody() : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération du service pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    if (userDTO != null && equipment != null && service != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO, service);
                    } else if (equipment != null && userDTO != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO);
                    } else {
                        return new IncidentWithEquipmentDTO(incident, equipment);
                    }
                })
                .collect(Collectors.toList());
    }


    public List<IncidentWithEquipmentDTO> getIncidentsByHospitalIdAndServiceId(String hospitalId, String serviceId) {
        return incidentRepository.findAllByHospitalIdAndServiceId(hospitalId, serviceId).stream()
                .map(incident -> {
                    Equipment equipment = incident.getEquipmentId() != null
                            ? equipmentRepository.findById(incident.getEquipmentId()).orElse(null)
                            : null;

                    UserDTO userDTO = null;
                    try {
                        userDTO = incident.getReportedBy() != null
                                ? userServiceClient.getUserById(token, incident.getReportedBy())
                                : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération de l'utilisateur pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    HospitalServiceEntity service = null;
                    try {
                        ResponseEntity<HospitalServiceEntity> serviceResponse = incident.getServiceId() != null
                                ? hospitalServiceClient.getServiceById(token, incident.getServiceId())
                                : null;
                        service = serviceResponse != null ? serviceResponse.getBody() : null;
                    } catch (Exception e) {
                        log.error("Erreur lors de la récupération du service pour l'incident {} : {}", incident.getId(), e.getMessage());
                    }

                    if (userDTO != null && equipment != null && service != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO, service);
                    } else if (equipment != null && userDTO != null) {
                        return new IncidentWithEquipmentDTO(incident, equipment, userDTO);
                    } else {
                        return new IncidentWithEquipmentDTO(incident, equipment);
                    }
                })
                .collect(Collectors.toList());
    }

    private void createCorrectiveMaintenanceForSevereIncident(Incident incident, Equipment equipment) {
        SLA sla = slaService.getSlaById(equipment.getSlaId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));

        CorrectiveMaintenance maintenance = CorrectiveMaintenance.builder()
                .equipmentId(equipment.getId())
                .incidentId(incident.getId())
                .assignedTo(sla.getUserIdCompany())
                .description(incident.getDescription())
                .plannedDate(incident.getReportedAt())
                .status("Planifié")
                .resolutionDetails("")
                .build();

        correctiveMaintenanceRepository.save(maintenance);
    }


    public Incident updateIncident(String incidentId, IncidentDTO updatedIncidentData, UserDTO user) {
        // Recherche de l'incident existant
        Incident existingIncident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé avec l'ID : " + incidentId));

        Equipment equipment = equipmentRepository.findById(existingIncident.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));

        // Ancien et nouveau statut
        String oldStatus = existingIncident.getStatus();
        String newStatus = updatedIncidentData.getStatus();

        // Conversion de la sévérité
        Severity severityEnum = convertToSeverity(updatedIncidentData.getSeverity());

        // Mise à jour des champs liés aux transitions de statut
        if ("Résolu".equals(oldStatus) && !"Résolu".equals(newStatus)) {
            existingIncident.setResolvedBy(null);
            existingIncident.setResolvedAt(null);
            equipment.setStatus("en maintenance");
            equipmentRepository.save(equipment);
        }

        if ("Résolu".equals(oldStatus) && "En attente".equals(newStatus)) {
            existingIncident.setResolvedBy(null);
            existingIncident.setResolvedAt(null);
            existingIncident.setValidatedBy(null);
            existingIncident.setValidatedAt(null);
            equipment.setStatus("en panne");
            equipmentRepository.save(equipment);
        }

        if ("En cours".equals(oldStatus) && "En attente".equals(newStatus)) {
            existingIncident.setValidatedBy(null);
            existingIncident.setValidatedAt(null);
            equipment.setStatus("en panne");
            equipmentRepository.save(equipment);
        }
        if ("En cours".equals(oldStatus) && "Résolu".equals(newStatus)) {
            existingIncident.setValidatedBy(null);
            existingIncident.setValidatedAt(null);
            equipment.setStatus("en service");
            equipmentRepository.save(equipment);

        }

        if ("En attente".equals(oldStatus) && "En cours".equals(newStatus)) {
            existingIncident.setValidatedBy(updatedIncidentData.getValidatedBy());
            existingIncident.setValidatedAt(LocalDateTime.now());
            equipment.setStatus("en maintenance");
            equipmentRepository.save(equipment);
        }

        if ("En attente".equals(oldStatus) && "Résolu".equals(newStatus)) {
            existingIncident.setResolvedBy(updatedIncidentData.getResolvedBy());
            existingIncident.setResolvedAt(LocalDateTime.now());
            existingIncident.setValidatedBy(updatedIncidentData.getValidatedBy());
            equipment.setStatus("en service");
            equipmentRepository.save(equipment);


        }

        // Mise à jour des autres champs
        if (updatedIncidentData.getDescription() != null) {
            existingIncident.setDescription(updatedIncidentData.getDescription());
        }

        if (updatedIncidentData.getPenaltyApplied() != 0) {
            existingIncident.setPenaltyApplied(updatedIncidentData.getPenaltyApplied());
        }

        if (updatedIncidentData.getResolutionDetails() != null) {
            existingIncident.setResolutionDetails(updatedIncidentData.getResolutionDetails());
        }

        existingIncident.setSeverity(severityEnum);
        existingIncident.setStatus(newStatus);

        // Récupérer les superviseurs du service où se trouve l’équipement
        List<UserDTO> serviceSupervisors = userServiceClient.getServiceSupervisors(token, equipment.getServiceId());
        List<String> emailsToNotify = serviceSupervisors.stream()
                .map(UserDTO::getEmail)
                .collect(Collectors.toList());

        // Ajouter l’initiateur de l’action
        emailsToNotify.add(user.getEmail());
        String hospitalId = equipment.getHospitalId();

        // Récupérer les utilisateurs du service via UserServiceClient avec le token
        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(token, hospitalId,
                List.of("ROLE_HOSPITAL_ADMIN",  "ROLE_MAINTENANCE_ENGINEER"));
        emailsToNotify.addAll(users.stream()
                .map(UserDTO::getEmail)
                .toList());

        // Créer l’objet de notification personnalisé
        NotificationEvent notificationEvent = new NotificationEvent(
                "Mise à jour de l'incident",
                "Le statut de l’incident concernant l’équipement '" + equipment.getNom() +
                        "' (Code: " + equipment.getSerialCode() + ") est passé de '" + oldStatus + "' à '" + newStatus + "'.",
                emailsToNotify
        );

        // Créer un événement métier spécifique pour les mails détaillés
        IncidentStatusUpdateEvent statusEvent = new IncidentStatusUpdateEvent(
                existingIncident.getId(),
                equipment.getSerialCode(),
                equipment.getNom(),
                oldStatus,
                newStatus,
                updatedIncidentData.getValidatedBy() != null ? updatedIncidentData.getValidatedBy() : updatedIncidentData.getResolvedBy(),
                LocalDateTime.now(),
                emailsToNotify
        );

        // Envoyer les événements Kafka
        kafkaProducerService.sendMessage("notification-events", notificationEvent);
        kafkaProducerService.sendMessage("incident-status-update-events", statusEvent);

        // Notifier selon la sévérité
        if (severityEnum == Severity.MAJEUR) {
            notifyCompany(equipment , user.getId());
        } else if (severityEnum == Severity.MODERE) {
            sendNotificationToCompanyForModerateIssue(equipment , user.getId());
        }


        if  (severityEnum == Severity.MODERE || severityEnum == Severity.MAJEUR) {
            createCorrectiveMaintenanceForSevereIncident(existingIncident, equipment);
            notifyCorrectiveMaintenanceCreation(equipment, user.getId());

        }


        return incidentRepository.save(existingIncident);
    }




    @Transactional
    public Incident validateIncident(String incidentId, String engineerId, String severity, IncidentDTO updatedIncidentData) {
        Severity severityEnum = convertToSeverity(severity);

        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

        Equipment equipment = equipmentRepository.findById(incident.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Équipement non trouvé"));
        UserDTO userDTO = userServiceClient.getUserById(token , engineerId);

        // Met à jour les données de l'incident via la méthode updateIncidentData
        Incident validatedIncident = updateIncident(incidentId, updatedIncidentData ,userDTO );
        validatedIncident.setValidatedBy(engineerId);
        validatedIncident.setStatus("En cours");
        validatedIncident.setSeverity(severityEnum);
        validatedIncident.setValidatedAt(LocalDateTime.now());

        equipment.setStatus("en maintenance");
        equipmentRepository.save(equipment);
        incidentRepository.save(validatedIncident);

        // Si la panne est MAJEURE, on notifie la société de maintenance
        if (severityEnum == Severity.MAJEUR) {
            notifyCompany(equipment , engineerId);
        }

        // Si la panne est MODEREE, vous pouvez aussi envoyer une notification à la société
        if (severityEnum == Severity.MODERE) {
            sendNotificationToCompanyForModerateIssue(equipment , engineerId);
        }

        if  (severityEnum == Severity.MODERE || severityEnum == Severity.MAJEUR) {
            createCorrectiveMaintenanceForSevereIncident(incident, equipment);
            notifyCorrectiveMaintenanceCreation(equipment, engineerId);

        }

        return validatedIncident;
    }

    private Severity convertToSeverity(String severity) {
        return switch (severity.toLowerCase()) {
            case "mineur" -> Severity.MINEUR;
            case "modere" -> Severity.MODERE;
            case "majeur" -> Severity.MAJEUR;
            default -> throw new IllegalArgumentException("Sévérité inconnue : " + severity);
        };
    }


    private void notifyCompany(Equipment equipment ,  String engineerId) {
        List<String> emails = new ArrayList<>();

        SLA sla = slaService.getSlaById(equipment.getSlaId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));

        UserDTO companyUser = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ", sla.getUserIdCompany());
        emails.add(companyUser.getEmail());

        // Ajout ingénieur
        UserDTO engineer = userServiceClient.getUserById(token, engineerId);
        emails.add(engineer.getEmail());
        NotificationEvent event = new NotificationEvent(
                "Validation d’incident",
                "L'incident sur l'équipement " + equipment.getNom() + " a été validé avec succès.",
                emails
        );

        kafkaProducerService.sendMessage("notification-events", event);
        log.info("Notification envoyée à la société pour un incident MAJEUR sur l'équipement : " + equipment.getSerialCode());
    }

    private void sendNotificationToCompanyForModerateIssue(Equipment equipment , String engineerId) {
        List<String> emails = new ArrayList<>();

        SLA sla = slaService.getSlaById(equipment.getSlaId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));

        UserDTO companyUser = userServiceClient.getUserById("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ", sla.getUserIdCompany());
        emails.add(companyUser.getEmail());
        // Ajout ingénieur
        UserDTO engineer = userServiceClient.getUserById(token, engineerId);
        emails.add(engineer.getEmail());

        NotificationEvent notificationEvent = new NotificationEvent(
                "Incident modéré validé",
                "Un incident modéré a été validé sur l’équipement : " + equipment.getSerialCode(),
                emails
        );

        kafkaProducerService.sendMessage("notification-events", notificationEvent);
        log.info("Notification envoyée à la société pour un incident modéré sur l'équipement : " + equipment.getSerialCode());
    }


    private void notifyCorrectiveMaintenanceCreation(Equipment equipment, String engineerId) {
        List<String> emails = new ArrayList<>();

        // Récupérer la société via SLA
        SLA sla = slaService.getSlaById(equipment.getSlaId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));
        UserDTO companyUser = userServiceClient.getUserById(token, sla.getUserIdCompany());
        emails.add(companyUser.getEmail());

        // Récupérer l’ingénieur
        UserDTO engineer = userServiceClient.getUserById(token, engineerId);
        emails.add(engineer.getEmail());

        // Récupérer les administrateurs de l’hôpital
        List<UserDTO> hospitalAdmins = userServiceClient.getUsersByHospitalAndRoles(
                token,
                equipment.getHospitalId(),
                List.of("ROLE_HOSPITAL_ADMIN")
        );
        emails.addAll(hospitalAdmins.stream()
                .map(UserDTO::getEmail)
                .toList());

        NotificationEvent notification = new NotificationEvent(
                "Maintenance corrective planifiée",
                "Une opération de maintenance corrective a été créée pour l’équipement : " +
                        equipment.getNom() + " (Code : " + equipment.getSerialCode() + ").",
                emails
        );

        kafkaProducerService.sendMessage("notification-events", notification);
        log.info("Notification envoyée à la société, l'ingénieur et l'admin de l’hôpital pour la maintenance corrective de l’équipement : " + equipment.getSerialCode());
    }

    public void deleteIncidentById(String incidentId) {
        incidentRepository.deleteById(incidentId);
    }

}
