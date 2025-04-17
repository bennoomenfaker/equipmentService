package platformMedical.equipment_service.controller;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.service.EmdnNomenclatureService;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;


@RestController
@RequestMapping("/api/emdn")
@RequiredArgsConstructor
public class EmdnNomenclatureController {

    private final EmdnNomenclatureService emdnNomenclatureService;


    // Endpoint pour récupérer toutes les nomenclatures
    @GetMapping("/nomenclatures")
    public List<EmdnNomenclature> getAllNomenclatures() {
        return emdnNomenclatureService.getAllNomenclatures();
    }





}



