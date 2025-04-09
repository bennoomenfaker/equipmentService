package platformMedical.equipment_service.entity.DTOs;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEvent {
    private String title; // Titre de la notification
    private String message; // Contenu de la notification
    private List<String> recipients; // Liste des emails des destinataires
}
