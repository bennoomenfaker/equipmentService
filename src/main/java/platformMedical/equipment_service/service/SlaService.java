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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
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
                equipment.getSupplier().getName(),
                equipment.getAcquisitionDate(),
                equipment.getServiceId(),
                equipment.getBrand().getName(),
                equipment.getSlaId(),
                equipment.getStartDateWarranty(),
                equipment.getEndDateWarranty(),
                equipment.isReception(),
                equipment.getStatus(),
                equipment.getUseCount(),
                equipment.getUsageDuration(),
                equipment.getLastUsedAt(),
                equipment.isFromMinistere()
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
    public List<SLADetailsDTO> getSLAsWithEquipmentByHospital(String hospitalId) {
        List<SLA> slas = slaRepository.findByHospitalId(hospitalId);
        List<SLADetailsDTO> results = new ArrayList<>();

        for (SLA sla : slas) {
            Equipment equipment = equipmentRepository.findById(sla.getEquipmentId()).orElse(null);
            if (equipment != null) {
                results.add(
                        SLADetailsDTO.builder()
                                .sla(sla)
                                .equipmentNom(equipment.getNom())
                                .serialCode(equipment.getSerialCode())
                                .build()
                );
            }
        }
        return results;
    }






    // Envoyer une notification de violation du SLA


    private void sendSlaViolationNotification(Incident incident, SLA sla, Equipment equipment, double newPenalty) {
        List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(
                token,
                equipment.getHospitalId(),
                List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MAINTENANCE_ENGINEER", "ROLE_COMPANY_MAINTENANCE")
        );

        List<String> emails = users.stream()
                .map(UserDTO::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        String hospitalName = hospitalServiceClient.getHospitalNameById(token, incident.getHospitalId());

        String content = String.format(
                "⚠️ SLA violé\n- Équipement : %s (%s)\n- Hôpital : %s\n- Nouvelle pénalité appliquée : %.2f DT\n- Pénalité cumulée : %.2f DT\n- Statut : %s",
                equipment.getNom(), equipment.getSerialCode(), hospitalName, newPenalty, incident.getPenaltyApplied(), incident.getStatus()
        );

        NotificationEvent notif = new NotificationEvent(
                "Alerte SLA - " + incident.getStatus(),
                content,
                emails
        );

        kafkaProducerService.sendMessage("sla-warning-events", notif);
    }

//    public String checkSlaCompliance(String incidentId) {
//        // Récupérer l'incident
//        Incident incident = incidentRepository.findById(incidentId)
//                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));
//
//        // Vérifier si une pénalité a déjà été appliquée
//        if (incident.getPenaltyApplied() > 0) {
//            // Récupérer le SLA associé à l'équipement
//            Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
//            if (!slaOpt.isPresent()) {
//                log.warn("Aucun SLA trouvé pour l'équipement : " + incident.getEquipmentId());
//                return "Pénalité déjà appliquée (" + incident.getPenaltyApplied() + " dt), mais aucun SLA n'a été trouvé pour l'équipement concerné.\n";
//            }
//
//            SLA sla = slaOpt.get();
//            double responseHoursLate = 0;
//            double resolutionHoursLate = 0;
//
//            if (incident.getValidatedAt() != null && sla.getMaxResponseTime() > 0) {
//                long minutesResponse = Duration.between(incident.getReportedAt(), incident.getValidatedAt()).toMinutes();
//                if (minutesResponse > sla.getMaxResponseTime()) {
//                    responseHoursLate = (minutesResponse - sla.getMaxResponseTime()) / 60.0;
//                }
//            }
//
//            if (incident.getResolvedAt() != null && sla.getMaxResolutionTime() > 0) {
//                long minutesResolution = Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes();
//                if (minutesResolution > sla.getMaxResolutionTime()) {
//                    resolutionHoursLate = (minutesResolution - sla.getMaxResolutionTime()) / 60.0;
//                }
//            }
//
//            double penaltyResponse = Math.round(responseHoursLate * sla.getPenaltyAmount() * 100.0) / 100.0;
//            double penaltyResolution = Math.round(resolutionHoursLate * sla.getPenaltyAmount() * 100.0) / 100.0;
//            double totalPenalty = Math.round((penaltyResponse + penaltyResolution) * 100.0) / 100.0;
//
//            return "Pénalité déjà appliquée : " + incident.getPenaltyApplied() + " dt.\n"
//                    + "- Retard réponse : " + String.format("%.2f", responseHoursLate) + " h × " + sla.getPenaltyAmount() + " dt/h = " + penaltyResponse + " dt\n"
//                    + "- Retard résolution : " + String.format("%.2f", resolutionHoursLate) + " h × " + sla.getPenaltyAmount() + " dt/h = " + penaltyResolution + " dt\n"
//                    + "- Montant total calculé : " + totalPenalty + " dt\n";
//        }
//
//
//        // Récupérer le SLA associé à l'équipement
//        Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
//        if (!slaOpt.isPresent()) {
//            log.warn("Aucun SLA trouvé pour l'équipement : " + incident.getEquipmentId());
//        }
//        SLA sla = slaOpt.orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé pour l'équipement : " + incident.getEquipmentId()));
//
//        double totalPenalty = 0.0;
//        boolean responseViolated = false;
//        boolean resolutionViolated = false;
//        StringBuilder message = new StringBuilder();
//
//        // Vérification du délai de réponse
//        if (incident.getValidatedAt() != null && sla.getMaxResponseTime() > 0) {
//            long minutesResponse = Duration.between(incident.getReportedAt(), incident.getValidatedAt()).toMinutes();
//            if (minutesResponse > sla.getMaxResponseTime()) {
//                long minutesLate = minutesResponse - sla.getMaxResponseTime();
//                double hoursLate = minutesLate / 60.0;
//                double penalty = Math.round(hoursLate * sla.getPenaltyAmount() * 100.0) / 100.0; // Arrondi à 2 décimales
//                totalPenalty += penalty;
//                responseViolated = true;
//
//                // Conversion du retard en jours, heures ou minutes
//                String responseTimeLate = getTimeString(minutesLate);
//
//                // Message détaillant la violation de délai de réponse
//                message.append("La réponse à l'incident a été validée après ")
//                        .append(responseTimeLate)
//                        .append(" de retard. Délai de réponse maximal : ")
//                        .append(sla.getMaxResponseTime())
//                        .append(" minutes. Pénalité appliquée : ")
//                        .append(penalty)
//                        .append(" dt.\n");
//            }
//        }
//
//        // Vérification du délai de résolution
//        if (incident.getResolvedAt() != null && sla.getMaxResolutionTime() > 0) {
//            long minutesResolution = Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes();
//            if (minutesResolution > sla.getMaxResolutionTime()) {
//                long minutesLate = minutesResolution - sla.getMaxResolutionTime();
//                double hoursLate = minutesLate / 60.0;
//                double penalty = Math.round(hoursLate * sla.getPenaltyAmount() * 100.0) / 100.0; // Arrondi à 2 décimales
//                totalPenalty += penalty;
//                resolutionViolated = true;
//
//                // Conversion du retard en jours, heures ou minutes
//                String resolutionTimeLate = getTimeString(minutesLate);
//
//                // Message détaillant la violation de délai de résolution
//                message.append("La résolution de l'incident a pris ")
//                        .append(resolutionTimeLate)
//                        .append(" de retard. Délai de résolution maximal : ")
//                        .append(sla.getMaxResolutionTime())
//                        .append(" minutes. Pénalité appliquée : ")
//                        .append(penalty)
//                        .append(" dt.\n");
//            }
//        }
//
//        // Appliquer la pénalité et marquer les violations
//        if (totalPenalty > 0) {
//            incident.setPenaltyApplied(totalPenalty);
//        }
//
//        incident.setSlaResponseViolated(responseViolated);
//        incident.setSlaResolutionViolated(resolutionViolated);
//        incidentRepository.save(incident);
//
//        // Générer un message descriptif
//        if (!responseViolated && !resolutionViolated) {
//            message.append("Aucune violation de SLA détectée.");
//        }
//
//        // Retourner le message détaillé
//        return message.toString();
//    }

    public String checkSlaCompliance(String incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé avec l'ID : " + incidentId));

        Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
        if (slaOpt.isEmpty()) {
            throw new ResourceNotFoundException("Aucun SLA trouvé pour l'équipement : " + incident.getEquipmentId());
        }
        SLA sla = slaOpt.get();
        StringBuilder message = new StringBuilder();

        // Si pénalité déjà appliquée, afficher détails + ne pas recalculer
        if (incident.getPenaltyApplied() > 0) {
            message.append("Pénalité déjà appliquée : ").append(incident.getPenaltyApplied()).append(" dt.\n");
            message.append("Détails du calcul original :\n");
            message.append(calculateAndFormatPenaltyDetails(incident, sla));
            return message.toString();
        }

        String status = incident.getStatus();
        double totalPenalty = 0.0;
        boolean responseViolated = false;
        boolean resolutionViolated = false;

        // Vérifier le SLA de réponse :
        // Condition : Si on est "En attente" ou "Résolu" et que la réponse n'a pas été violée (on vérifie toujours en "Résolu")
        if ((status.equals("En attente") || status.equals("Résolu"))
                && !incident.isSlaResponseViolated()
                && incident.getReportedAt() != null
                && incident.getValidatedAt() != null) {

            long responseMinutes = Duration.between(incident.getReportedAt(), incident.getValidatedAt()).toMinutes();
            long maxResponseMinutes = sla.getMaxResponseTime() * 60L;

            if (responseMinutes > maxResponseMinutes) {
                long responseLate = responseMinutes - maxResponseMinutes;
                BigDecimal penaltyResponse = calculatePenalty(responseLate, sla.getPenaltyAmount());
                totalPenalty += penaltyResponse.doubleValue();
                responseViolated = true;

                message.append("Violation SLA réponse : retard de ")
                        .append(getTimeString(responseLate))
                        .append(". Pénalité : ")
                        .append(penaltyResponse)
                        .append(" dt.\n");
            }
        }

        // Vérifier le SLA de résolution :
        // Condition : Si on est "En cours" ou "Résolu" et que résolution non violée
        if ((status.equals("En cours") || status.equals("Résolu"))
                && !incident.isSlaResolutionViolated()
                && incident.getReportedAt() != null
                && incident.getResolvedAt() != null) {

            long resolutionMinutes = Duration.between(incident.getReportedAt(), incident.getResolvedAt()).toMinutes();
            long maxResolutionMinutes = sla.getMaxResolutionTime() * 60L;

            if (resolutionMinutes > maxResolutionMinutes) {
                long resolutionLate = resolutionMinutes - maxResolutionMinutes;
                BigDecimal penaltyResolution = calculatePenalty(resolutionLate, sla.getPenaltyAmount());
                totalPenalty += penaltyResolution.doubleValue();
                resolutionViolated = true;

                message.append("Violation SLA résolution : retard de ")
                        .append(getTimeString(resolutionLate))
                        .append(". Pénalité : ")
                        .append(penaltyResolution)
                        .append(" dt.\n");
            }
        }

        // Appliquer la pénalité cumulée et sauvegarder les flags
        if (totalPenalty > 0) {
            incident.setPenaltyApplied(totalPenalty);
            incident.setSlaResponseViolated(responseViolated);
            incident.setSlaResolutionViolated(resolutionViolated);
            incidentRepository.save(incident);
        } else {
            message.append("✅ Aucune violation SLA détectée pour le statut : ").append(status);
        }

        return message.toString();
    }

    private BigDecimal calculatePenalty(long minutesLate, double penaltyAmountPerUnit) {
        BigDecimal hoursLate = BigDecimal.valueOf(minutesLate)
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        // Convertir millimes en dinars
        BigDecimal penaltyAmountBd = BigDecimal.valueOf(penaltyAmountPerUnit)
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
        return hoursLate.multiply(penaltyAmountBd).setScale(2, RoundingMode.HALF_UP);
    }


    private String calculateAndFormatPenaltyDetails(Incident incident, SLA sla) {
        StringBuilder details = new StringBuilder();
        BigDecimal totalCalculatedPenalty = BigDecimal.ZERO;

        // Calcul et affichage des détails pour le temps de réponse
        if (incident.getReportedAt() != null && incident.getResolvedAt() != null) {
            Duration responseDuration = Duration.between(incident.getReportedAt(), incident.getValidatedAt());
            long responseMinutes = responseDuration.toMinutes();
            long maxResponseMinutes = (long) sla.getMaxResponseTime() * 60;

            if (responseMinutes > maxResponseMinutes) {
                long responseMinutesLate = responseMinutes - maxResponseMinutes;
                BigDecimal penalty = calculatePenalty(responseMinutesLate, sla.getPenaltyAmount());
                totalCalculatedPenalty = totalCalculatedPenalty.add(penalty);
                details.append("  - Temps de réponse : ").append(getTimeString(responseMinutes)).append(" (Max : ").append(getTimeString(maxResponseMinutes)).append(") -> Retard : ").append(getTimeString(responseMinutesLate)).append(", Pénalité : ").append(penalty).append(" dt.\n");
            }
        }

        // Calcul et affichage des détails pour le temps de résolution
        if (incident.getReportedAt() != null && incident.getResolvedAt() != null) {
            Duration resolutionDuration = Duration.between(incident.getReportedAt(), incident.getResolvedAt());
            long resolutionMinutes = resolutionDuration.toMinutes();
            long maxResolutionMinutes = (long) sla.getMaxResolutionTime() * 60;

            if (resolutionMinutes > maxResolutionMinutes) {
                long resolutionMinutesLate = resolutionMinutes - maxResolutionMinutes;
                BigDecimal penalty = calculatePenalty(resolutionMinutesLate, sla.getPenaltyAmount());
                totalCalculatedPenalty = totalCalculatedPenalty.add(penalty);
                details.append("  - Temps de résolution : ").append(getTimeString(resolutionMinutes)).append(" (Max : ").append(getTimeString(maxResolutionMinutes)).append(") -> Retard : ").append(getTimeString(resolutionMinutesLate)).append(", Pénalité : ").append(penalty).append(" dt.\n");
            }
        }

        details.append("  - Pénalité totale calculée : ").append(totalCalculatedPenalty).append(" dt.\n");
        return details.toString();
    }

    // Méthode utilitaire pour formater le temps

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


//    @Scheduled(fixedRate = 480000) // Toutes 8 minutes
//    public void monitorSlaDeadlines() {
//        List<Incident> openIncidents = incidentRepository.findByStatusIn(List.of("En attente", "En cours"));
//
//        for (Incident incident : openIncidents) {
//            SLA sla = slaRepository.findByEquipmentId(incident.getEquipmentId()).orElse(null);
//            if (sla == null) continue;
//
//            Equipment equipment = equipmentRepository.findById(incident.getEquipmentId()).orElse(null);
//            if (equipment == null) continue;
//
//            ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
//
//            boolean slaViolated = false;
//            double penalty = 0.0;
//            String violationType = "";
//
//            if ("En attente".equals(incident.getStatus()) && incident.getReportedAt() != null && !incident.isSlaResponseViolated()) {
//                long minutesElapsed = Duration.between(incident.getReportedAt(), now).toMinutes();
//                if (minutesElapsed > sla.getMaxResponseTime()) {
//                    long minutesLate = minutesElapsed - sla.getMaxResponseTime();
//                    double hoursLate = minutesLate / 60.0;
//                    penalty += hoursLate * sla.getPenaltyAmount();
//                    incident.setSlaResponseViolated(true);
//                    slaViolated = true;
//                    violationType = "réponse";
//                }
//            }
//
//            if ("En cours".equals(incident.getStatus()) && incident.getReportedAt() != null && !incident.isSlaResolutionViolated()) {
//                long minutesElapsed = Duration.between(incident.getReportedAt(), now).toMinutes();
//                if (minutesElapsed > sla.getMaxResolutionTime()) {
//                    long minutesLate = minutesElapsed - sla.getMaxResolutionTime();
//                    double hoursLate = minutesLate / 60.0;
//                    penalty += hoursLate * sla.getPenaltyAmount();
//                    incident.setSlaResolutionViolated(true);
//                    slaViolated = true;
//                    violationType = "résolution";
//                }
//            }
//
//            if (slaViolated) {
//                incident.setPenaltyApplied(incident.getPenaltyApplied() != 0 ? incident.getPenaltyApplied() + penalty : penalty);
//                incidentRepository.save(incident);
//
//                List<UserDTO> users = userServiceClient.getUsersByHospitalAndRoles(token, equipment.getHospitalId(),
//                        List.of("ROLE_HOSPITAL_ADMIN", "ROLE_MAINTENANCE_ENGINEER", "ROLE_COMPANY_MAINTENANCE"));
//
//                List<String> emailsToNotify = users.stream()
//                        .map(UserDTO::getEmail)
//                        .filter(Objects::nonNull)
//                        .collect(Collectors.toList());
//
//                String hospitalName = hospitalServiceClient.getHospitalNameById(token, incident.getHospitalId());
//
//                String emailContent = String.format(
//                        "Alerte SLA (%s dépassé)\n" +
//                                "- Équipement : %s (%s)\n" +
//                                "- Hôpital : %s\n" +
//                                "- Montant pénalité : %.2f DT\n" +
//                                "- Statut incident : %s",
//                        violationType,
//                        equipment.getNom(), equipment.getSerialCode(),
//                        hospitalName,
//                        penalty,
//                        incident.getStatus()
//                );
//
//                NotificationEvent notification = new NotificationEvent(
//                        "Alerte SLA - " + violationType.toUpperCase(),
//                        emailContent,
//                        emailsToNotify
//                );
//
//                NotificationEvent notificationEvent = new NotificationEvent(
//                        "Alerte SLA - " + violationType.toUpperCase(),
//                        "Le SLA de l'équipement \"" + equipment.getNom() + "\" a été violé (" + violationType + ").",
//                        emailsToNotify
//                );
//
//                kafkaProducerService.sendMessage("sla-warning-events", notification);
//                kafkaProducerService.sendMessage("notification-events", notificationEvent);
//            }
//        }
//    }

    private BigDecimal calculatePenaltyInHours(long totalMinutesLate, double penaltyRate) {
        BigDecimal hoursLate = BigDecimal.valueOf(totalMinutesLate).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        return hoursLate.multiply(BigDecimal.valueOf(penaltyRate)).setScale(2, RoundingMode.HALF_UP);
    }




    // Le Moniteur : ne fait que détecter, flagger et notifier
    @Scheduled(fixedRate = 480000) // toutes les 8 minutes
    public void monitorSlaDeadlines() {
        List<Incident> incidents = incidentRepository.findByStatusIn(List.of("En attente", "En cours"));

        for (Incident incident : incidents) {
            // Récupérer SLA lié à l'équipement
            Optional<SLA> slaOpt = slaRepository.findByEquipmentId(incident.getEquipmentId());
            if (slaOpt.isEmpty()) {
                // Pas de SLA, on passe à l'incident suivant
                continue;
            }
            SLA sla = slaOpt.get();

            // Récupérer Equipment lié à l'incident
            Optional<Equipment> equipmentOpt = equipmentRepository.findById(incident.getEquipmentId());
            if (equipmentOpt.isEmpty()) {
                continue; // Equipement manquant -> ignorer
            }
            Equipment equipment = equipmentOpt.get();

            // Vérifier que la date de signalement est présente
            if (incident.getReportedAt() == null) {
                continue;
            }

            boolean violationDetected = false;

            ZonedDateTime now = ZonedDateTime.now();

            // Détecter violation de réponse
            if ("En attente".equals(incident.getStatus()) && !incident.isSlaResponseViolated()) {
                long minutesPassed = Duration.between(incident.getReportedAt(), now).toMinutes();
                if (minutesPassed > sla.getMaxResponseTime() * 60L) {
                    incident.setSlaResponseViolated(true);
                    violationDetected = true;
                }
            }

            // Détecter violation de résolution
            if ("En cours".equals(incident.getStatus()) && !incident.isSlaResolutionViolated()) {
                long minutesPassed = Duration.between(incident.getReportedAt(), now).toMinutes();
                if (minutesPassed > sla.getMaxResolutionTime() * 60L) {
                    incident.setSlaResolutionViolated(true);
                    violationDetected = true;
                }
            }

            if (violationDetected) {
                incidentRepository.save(incident);
                // Envoi notification, pas de pénalité calculée ici, donc 0
                sendSlaViolationNotification(incident, sla, equipment, 0);
            }
        }
    }











    //1. Suivi du taux de conformité SLA
    public SlaComplianceStats getSlaComplianceStats(String hospitalId) {
        List<Incident> incidents = incidentRepository.findAllByHospitalId(hospitalId);

        long total = incidents.size();
        long respected = incidents.stream()
                .filter(i -> !i.isSlaResponseViolated() && !i.isSlaResolutionViolated())
                .count();

        long violatedResponse = incidents.stream().filter(Incident::isSlaResponseViolated).count();
        long violatedResolution = incidents.stream().filter(Incident::isSlaResolutionViolated).count();

        return SlaComplianceStats.builder()
                .totalIncidents(total)
                .slaRespected(total - (violatedResponse + violatedResolution))
                .responseViolated(violatedResponse)
                .resolutionViolated(violatedResolution)
                .complianceRate((double) respected / total * 100)
                .build();
    }

    //2. Montant total des pénalités appliquées (par hôpital ou prestataire)
    public double getTotalPenaltiesByHospital(String hospitalId) {
        List<Incident> incidents = incidentRepository.findAllByHospitalId(hospitalId);
        return incidents.stream()
                .mapToDouble(Incident::getPenaltyApplied)
                .sum();
    }
    public double getTotalPenaltiesByCompany(String companyId) {
        List<SLA> slas = slaRepository.findByUserIdCompany(companyId);
        Set<String> equipmentIds = slas.stream().map(SLA::getEquipmentId).collect(Collectors.toSet());
        List<Incident> incidents = incidentRepository.findByEquipmentIdIn(equipmentIds);
        return incidents.stream()
                .mapToDouble(Incident::getPenaltyApplied)
                .sum();
    }
    //4. Top 5 équipements avec le plus de pénalités
    public List<EquipmentPenaltyDTO> getTopPenalizedEquipments(String hospitalId) {
        List<Incident> incidents = incidentRepository.findAllByHospitalId(hospitalId);

        // Filtrer les incidents pénalisés
        Map<String, Double> penaltiesByEquipment = incidents.stream()
                .filter(i -> i.getPenaltyApplied() > 0)
                .collect(Collectors.groupingBy(Incident::getEquipmentId, Collectors.summingDouble(Incident::getPenaltyApplied)));

        // Récupérer tous les équipements concernés
        Set<String> equipmentIds = penaltiesByEquipment.keySet();
        List<Equipment> equipments = equipmentRepository.findByIdIn(equipmentIds);

        // Créer une map id → serialNumber
        Map<String, String> equipmentIdToSerial = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getSerialCode));

        // Retourner le top 5
        return penaltiesByEquipment.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    String equipmentId = entry.getKey();
                    String serial = equipmentIdToSerial.getOrDefault(equipmentId, "Inconnu");
                    return new EquipmentPenaltyDTO(serial, entry.getValue());
                })
                .collect(Collectors.toList());
    }




}
