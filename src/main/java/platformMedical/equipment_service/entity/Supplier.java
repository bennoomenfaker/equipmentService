package platformMedical.equipment_service.entity;


import lombok.*;
import org.springframework.data.annotation.Id;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {
    @Id
    private String id;
    private String name;
    private String hospitalId;
    private String email;
    private String tel;
    private boolean activated = true;
}
