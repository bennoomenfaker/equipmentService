package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.DTOs.*;
import platformMedical.equipment_service.entity.Equipment;
import platformMedical.equipment_service.entity.Incident;
import platformMedical.equipment_service.entity.SLA;
import platformMedical.equipment_service.kafka.KafkaProducerService;
import platformMedical.equipment_service.repository.EquipmentRepository;
import platformMedical.equipment_service.repository.IncidentRepository;
import platformMedical.equipment_service.repository.SLARepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class SlaService {

    private final SLARepository slaRepository;
    private final IncidentRepository incidentRepository;
    private final EquipmentRepository equipmentRepository;
    private final KafkaProducerService kafkaProducerService;
    private final UserServiceClient userServiceClient;
    private final String token = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmYWtlcmJlbm5vb21lbkBnbWFpbC5jb20iLCJyb2xlIjoiUk9MRV9IT1NQSVRBTF9BRE1JTiIsImlhdCI6MTc0Mzg0NjM1MCwiZXhwIjoyMDU5MjA2MzUwfQ.BXDfRfGt5_zWvYwDe_ukWf2pQUgTLZxMHxX2INXaGbQ";
    private final HospitalServiceClient hospitalServiceClient;


    //  Créer un SLA
    public SLA createSla(SLA sla) {
        return slaRepository.save(sla);
    }

    //  Récupérer un SLA par ID
    public Optional<SLA> getSlaById(String slaId) {
        return slaRepository.findById(slaId);
    }

    //  Récupérer le SLA d'un équipement
    public Optional<SLA> getSlaByEquipmentId(String equipmentId) {
        return slaRepository.findByEquipmentId(equipmentId);
    }

    //  Lister les SLA d'un prestataire de maintenance
    public List<SlaWithEquipmentDTO> getSlasWithEquipmentByCompany(String userIdCompany) {
        List<SLA> slas = slaRepository.findByUserIdCompany(userIdCompany);

        return slas.stream().map(sla -> {
            Equipment equipment = equipmentRepository.findById(sla.getEquipmentId()).orElse(null);
           EquipmentRequest equipmentRequest = mapToEquipmentRequest(equipment);
            return new SlaWithEquipmentDTO(
                    sla.getId(),
                    sla.getName(),
                    sla.getMaxResponseTime(),
                    sla.getMaxResolutionTime(),
                    sla.getPenaltyAmount(),
                    sla.getHospitalId(),
                    sla.getUserIdCompany(),
                    equipmentRequest
            );
        }).collect(Collectors.toList());
    }
    private EquipmentRequest mapToEquipmentRequest(Equipment equipment) {
        if (equipment == null) return null;

        return new EquipmentRequest(
                equipment.getNom(),
                equipment.getSerialCode(),
                equipment.getLifespan(),
                equipment.getRiskClass(),
                equipment.getHospitalId(),
                equipment.getSerialCode(),
                equipment.getAmount(),
                equipment.getSupplier(),
                equipment.getAcquisitionDate(),
                equipment.getServiceId(),
                equipment.getBrand().getName(),
                equipment.getSparePartIds(),  // ou new ArrayList<>(equipment.getSparePartIds()) si besoin
                equipment.getSlaId(),
                equipment.getStartDateWarranty(),
                equipment.getEndDateWarranty(),
                equipment.isReception(),
                equipment.getStatus()
        );
    }



    // Mise à jour d'un SLA
    public SLA updateSla(String slaId, SLA updatedSla) {
        // Vérifier si le SLA existe
        Optional<SLA> existingSla = slaRepository.findById(slaId);
        if (existingSla.isPresent()) {
            SLA sla = existingSla.get();

            // Mettre à jour les champs du SLA avec les nouvelles données
            sla.setName(updatedSla.getName());  // Par exemple, mettre à jour la description
            sla.setEquipmentId(updatedSla.getEquipmentId());
            sla.setPenaltyAmount(updatedSla.getPenaltyAmount());
            sla.setMaxResponseTime(updatedSla.getMaxResponseTime());
            sla.setMaxResolutionTime(updatedSla.getMaxResolutionTime());
            sla.setUserIdCompany(updatedSla.getUserIdCompany());  // Mettre à jour le prestataire

            // Vous pouvez ajouter d'autres champs à mettre à jour ici

            return slaRepository.save(sla);  // Sauvegarder et retourner le SLA mis à jour
        } else {
            throw new RuntimeException("SLA non trouvé");
        }
    }


    //  Supprimer un SLA
    public void deleteSla(String slaId) {
        slaRepository.deleteById(slaId);
    }




    // Récupérer les SLA associés à un hôpital
    public List<SLA> getSlasByHospital(String hospitalId) {
        return slaRepository.findByHospitalId(hospitalId);
    }







    // Envoyer une notification de violation du SLA
    private void sendSlaViolationNotification(Incident incident, String message) {
        // Récupérer les emails des utilisateurs à notifier
        List<String> emailsToNotify = new ArrayList<>();

        // Ajoutez ici les utilisateurs à notifier, par exemple le responsable SLA, l'ingénieur concerné, etc.

        // Créer l'événement Kafka pour notifier
        NotificationEvent notificationEvent = new NotificationEvent(
                message,
                "L'incident avec l'équipement " + incident.getEquipmentId() + " a violé un SLA : " + message,
                emailsToNotify
        );

        kafkaProducerService.sendMessage("sla-violation-events", notificationEvent);
    }

    public String checkSlaCompliance(String incidentId) {
        // Récupérer l'incident
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

        // Vérifier si une pénalité a déjà été appliquée
        if (incident.getPenaltyApplied() > 0) {
            // Récupérer le SLA associé à l'équipement
            Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
            if (!slaOpt.isPresent()) {
                log.warn("Aucun SLA trouvé pour l'équipement : " + incident.getEquipmentId());
                return "Pénalité déjà appliquée : " + incident.getPenaltyApplied() + " dt.\n";
            }

            SLA sla = slaOpt.get();
            double responseHoursLate = 0;
            double resolutionHoursLate = 0;

            if (incident.getValidatedAt() != null && sla.getMaxResponseTime() > 0) {
                long minutesResponse = Duration.between(incident.getReportedAt(), incident.getValidatedAt()).toMinutes();
                if (minutesResponse > sla.getMaxResponseTime()) {
                    responseHoursLate = (minutesResponse - sla.getMaxResponseTime()) / 60.0;
                }
            }

            if (incident.getResolvedAt() != null && sla.getMaxResolutionTime() > 0) {
                long minutesResolution = Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes();
                if (minutesResolution > sla.getMaxResolutionTime()) {
                    resolutionHoursLate = (minutesResolution - sla.getMaxResolutionTime()) / 60.0;
                }
            }

            double penaltyResponse = Math.round(responseHoursLate * sla.getPenaltyAmount() * 100.0) / 100.0;
            double penaltyResolution = Math.round(resolutionHoursLate * sla.getPenaltyAmount() * 100.0) / 100.0;
            double totalPenalty = Math.round((penaltyResponse + penaltyResolution) * 100.0) / 100.0;

            return "Pénalité déjà appliquée : " + incident.getPenaltyApplied() + " dt.\n"
                    + "- Retard réponse : " + String.format("%.2f", responseHoursLate) + " h × " + sla.getPenaltyAmount() + " dt/h = " + penaltyResponse + " dt\n"
                    + "- Retard résolution : " + String.format("%.2f", resolutionHoursLate) + " h × " + sla.getPenaltyAmount() + " dt/h = " + penaltyResolution + " dt\n"
                    + "- Montant total calculé : " + totalPenalty + " dt\n";
        }


        // Récupérer le SLA associé à l'équipement
        Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
        if (!slaOpt.isPresent()) {
            log.warn("Aucun SLA trouvé pour l'équipement : " + incident.getEquipmentId());
        }
        SLA sla = slaOpt.orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé pour l'équipement : " + incident.getEquipmentId()));

        double totalPenalty = 0.0;
        boolean responseViolated = false;
        boolean resolutionViolated = false;
        StringBuilder message = new StringBuilder();

        // Vérification du délai de réponse
        if (incident.getValidatedAt() != null && sla.getMaxResponseTime() > 0) {
            long minutesResponse = Duration.between(incident.getReportedAt(), incident.getValidatedAt()).toMinutes();
            if (minutesResponse > sla.getMaxResponseTime()) {
                long minutesLate = minutesResponse - sla.getMaxResponseTime();
                double hoursLate = minutesLate / 60.0;
                double penalty = Math.round(hoursLate * sla.getPenaltyAmount() * 100.0) / 100.0; // Arrondi à 2 décimales
                totalPenalty += penalty;
                responseViolated = true;

                // Conversion du retard en jours, heures ou minutes
                String responseTimeLate = getTimeString(minutesLate);

                // Message détaillant la violation de délai de réponse
                message.append("La réponse à l'incident a été validée après ")
                        .append(responseTimeLate)
                        .append(" de retard. Délai de réponse maximal : ")
                        .append(sla.getMaxResponseTime())
                        .append(" minutes. Pénalité appliquée : ")
                        .append(penalty)
                        .append(" dt.\n");
            }
        }

        // Vérification du délai de résolution
        if (incident.getResolvedAt() != null && sla.getMaxResolutionTime() > 0) {
            long minutesResolution = Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes();
            if (minutesResolution > sla.getMaxResolutionTime()) {
                long minutesLate = minutesResolution - sla.getMaxResolutionTime();
                double hoursLate = minutesLate / 60.0;
                double penalty = Math.round(hoursLate * sla.getPenaltyAmount() * 100.0) / 100.0; // Arrondi à 2 décimales
                totalPenalty += penalty;
                resolutionViolated = true;

                // Conversion du retard en jours, heures ou minutes
                String resolutionTimeLate = getTimeString(minutesLate);

                // Message détaillant la violation de délai de résolution
                message.append("La résolution de l'incident a pris ")
                        .append(resolutionTimeLate)
                        .append(" de retard. Délai de résolution maximal : ")
                        .append(sla.getMaxResolutionTime())
                        .append(" minutes. Pénalité appliquée : ")
                        .append(penalty)
                        .append(" dt.\n");
            }
        }

        // Appliquer la pénalité et marquer les violations
        if (totalPenalty > 0) {
            incident.setPenaltyApplied(totalPenalty);
        }

        incident.setSlaResponseViolated(responseViolated);
        incident.setSlaResolutionViolated(resolutionViolated);
        incidentRepository.save(incident);

        // Générer un message descriptif
        if (!responseViolated && !resolutionViolated) {
            message.append("Aucune violation de SLA détectée.");
        }

        // Retourner le message détaillé
        return message.toString();
    }

    // Méthode pour convertir les minutes en jours, heures ou minutes
    private String getTimeString(long minutes) {
        if (minutes >= 1440) { // Si plus de 24 heures
            long days = minutes / 1440;
            return days + " jour(s)";
        } else if (minutes >= 60) { // Si plus d'une heure
            long hours = minutes / 60;
            return hours + " heure(s)";
        } else { // Si moins d'une heure
            return minutes + " minute(s)";
        }
    }


    @Scheduled(fixedRate = 480000) // Toutes 8 minutes
    public void monitorSlaDeadlines() {
        List<Incident> openIncidents = incidentRepository.findByStatusIn(List.of("En attente", "En cours"));

        for (Incident incident : openIncidents) {
            SLA sla = slaRepository.findByEquipmentId(incident.getEquipmentId()).orElse(null);
            if (sla == null) continue;

            Equipment equipment = equipmentRepository.findById(incident.getEquipmentId()).orElse(null);
            if (equipment == null) continue;

            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());

            boolean slaViolated = false;
            double penalty = 0.0;
            String violationType = "";

            if ("En attente".equals(incident.getStatus()) && incident.getReportedAt() != null && !incident.isSlaResponseViolated()) {
                long minutesElapsed = Duration.between(incident.getReportedAt(), now).toMinutes();
                if (minutesElapsed > sla.getMaxResponseTime()) {
                    long minutesLate = minutesElapsed - sla.getMaxResponseTime();
                    double hoursLate = minutesLate / 60.0;
                    penalty += hoursLate * sla.getPenaltyAmount();
                    incident.setSlaResponseViolated(true);
                    slaViolated = true;
                    violationType = "réponse";
                }
            }

            if ("En cours".equals(incident.getStatus()) && incident.getReportedAt() != null && !incident.isSlaResolutionViolated()) {
                long minutesElapsed = Duration.between(incident.getReportedAt(), now).toMinutes();
                if (minutesElapsed > sla.getMaxResolutionTime()) {
                    long minutesLate = minutesElapsed - sla.getMaxResolutionTime();
                    double hoursLate = minutesLate / 60.0;
                    penalty += hoursLate * sla.getPenaltyAmount();
                    incident.setSlaResolutionViolated(true);
                    slaViolated = true;
                    violationType = "résolution";
                }
            }

            if (slaViolated) {
                incident.setPenaltyApplied(incident.getPenaltyApplied() != 0 ? incident.getPenaltyApplied() + penalty : penalty);
                incidentRepository.save(incident);

                List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(token, equipment.getHospitalId(),
                        List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MAINTENANCE_ENGINEER", "ROLE_COMPANY_MAINTENANCE"));

                List<String> emailsToNotify = users.stream()
                        .map(UserDTO::getEmail)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                String hospitalName = hospitalServiceClient.getHospitalNameById(token, incident.getHospitalId());

                String emailContent = String.format(
                        "Alerte SLA (%s dépassé)\n" +
                                "- Équipement : %s (%s)\n" +
                                "- Hôpital : %s\n" +
                                "- Montant pénalité : %.2f DT\n" +
                                "- Statut incident : %s",
                        violationType,
                        equipment.getNom(), equipment.getSerialCode(),
                        hospitalName,
                        penalty,
                        incident.getStatus()
                );

                NotificationEvent notification = new NotificationEvent(
                        "Alerte SLA - " + violationType.toUpperCase(),
                        emailContent,
                        emailsToNotify
                );

                NotificationEvent notificationEvent = new NotificationEvent(
                        "Alerte SLA - " + violationType.toUpperCase(),
                        "Le SLA de l'équipement \"" + equipment.getNom() + "\" a été violé (" + violationType + ").",
                        emailsToNotify
                );

                kafkaProducerService.sendMessage("sla-warning-events", notification);
                kafkaProducerService.sendMessage("notification-events", notificationEvent);
            }
        }
    }

}
