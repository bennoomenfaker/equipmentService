package platformMedical.equipment_service.service;

import lombok.AllArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.DTOs.NotificationEvent;
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
import java.util.Optional;

@Service
@AllArgsConstructor
public class SlaService {

    private final SLARepository slaRepository;
    private final IncidentRepository incidentRepository;
    private final KafkaProducerService kafkaProducerService;


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
    public List<SLA> getSlasByUserCompany(String userIdCompany) {
        return slaRepository.findByUserIdCompany(userIdCompany);
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




    // Vérifier si les délais de réponse et de résolution sont respectés
    public void checkSlaCompliance(String incidentId) {
        // Récupérer l'incident par son ID
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident non trouvé"));

        // Récupérer le SLA associé à l'équipement de l'incident
        SLA sla = slaRepository.findByEquipmentId(incident.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("SLA non trouvé"));

        // Vérifier le délai de réponse
        if (incident.getReportedAt() != null && sla.getMaxResponseTime() > 0) {
            // Convertir l'heure de signalement en ZonedDateTime (en supposant un fuseau horaire spécifique, ici system default)
            ZonedDateTime reportedAtZoned = incident.getReportedAt().atZone(ZoneId.systemDefault());
            long hoursElapsed = Duration.between(reportedAtZoned, ZonedDateTime.now(ZoneId.systemDefault())).toHours();
            if (hoursElapsed > sla.getMaxResponseTime()) {
                sendSlaViolationNotification(incident, "Délai de réponse dépassé");
            }
        }

        // Vérifier le délai de résolution
        if (incident.getResolvedAt() != null && sla.getMaxResolutionTime() > 0) {
            ZonedDateTime resolvedAtZoned = incident.getResolvedAt().atZone(ZoneId.systemDefault());
            long hoursElapsed = Duration.between(incident.getReportedAt().atZone(ZoneId.systemDefault()), resolvedAtZoned).toHours();
            if (hoursElapsed > sla.getMaxResolutionTime()) {
                sendSlaViolationNotification(incident, "Délai de résolution dépassé");
            }
        }
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
}
