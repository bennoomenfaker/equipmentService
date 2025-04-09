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
    /*
    // Endpoint pour récupérer une famille par son code (ex: Z)
    @GetMapping("/family/{code}")
    public Optional<EmdnNomenclature> getFamilyByCode(@PathVariable String code) {
        return emdnNomenclatureService.getFamilyByCode(code);
    }

    // Endpoint pour récupérer une sous-famille par le code de la famille (ex: Z) et le code de la sous-famille (ex: Z11)
    @GetMapping("/subfamily/{parentCode}/{code}")
    public Optional<EmdnNomenclature> getSubfamilyByCode(@PathVariable String parentCode, @PathVariable String code) {
        return emdnNomenclatureService.getSubfamilyByCode(parentCode, code);
    }

    // Endpoint pour récupérer un sous-type par les codes de la famille et sous-famille
    @GetMapping("/subtype/{parentCode}/{subfamilyCode}/{code}")
    public Optional<EmdnNomenclature> getSubtypeByCode(@PathVariable String parentCode, @PathVariable String subfamilyCode, @PathVariable String code) {
        return emdnNomenclatureService.getSubtypeByCode(parentCode, subfamilyCode, code);
    }


    // Endpoint pour récupérer un sous-sous-type par les codes de la famille et sous-famille et sous-sou-famille
    @GetMapping("/subtype/{parentCode}/{subfamilyCode}/{subSubfamilyCode}/{code}")
    public Optional<EmdnNomenclature> getSubSubtypeByCode(@PathVariable String parentCode, @PathVariable String subfamilyCode,@PathVariable String subSubfamilyCode, @PathVariable String code) {
        return emdnNomenclatureService.getSubsubtypeByCode(parentCode, subfamilyCode , subSubfamilyCode, code);
    }




    @GetMapping("/subsubfamilies/{parentCode}")
    public List<EmdnNomenclature> getSubsubfamilies(@PathVariable String parentCode) {
        return emdnNomenclatureService.getSubtypesByParentCode(parentCode);
    }
    */

}



