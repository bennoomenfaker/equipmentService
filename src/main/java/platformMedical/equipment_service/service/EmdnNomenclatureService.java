package platformMedical.equipment_service.service;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import platformMedical.equipment_service.entity.EmdnNomenclature;
import platformMedical.equipment_service.repository.EmdnNomenclatureRepository;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class EmdnNomenclatureService {

    private final EmdnNomenclatureRepository repository;


    // Récupérer toutes les nomenclatures
    public List<EmdnNomenclature> getAllNomenclatures() {
        return repository.findAll();
    }


/*
    // Récupérer une nomenclature par son code
    public Optional<EmdnNomenclature> getNomenclatureByCode(String code) {
        return repository.findByCode(code);
    }

    // Enregistrer une nomenclature
    public EmdnNomenclature saveNomenclature(EmdnNomenclature nomenclature) {
        return repository.save(nomenclature);
    }
    // Récupérer tous les sous-types de la famille ou sous-famille spécifiée
    public List<EmdnNomenclature> getAllSubtypesByCode(String code) {
        Optional<EmdnNomenclature> family = repository.findByCode(code);
        if (family.isPresent()) {
            return family.get().getSubtypes();
        }
        return List.of();
    }

    // Récupérer une famille et ses sous-familles (code: Z, par exemple)
    public Optional<EmdnNomenclature> getFamilyByCode(String code) {
        return repository.findByCode(code);
    }
     //2 eme niveaux
    // Récupérer une sous-famille spécifique à partir d'une famille (code: Z11)
    public Optional<EmdnNomenclature> getSubfamilyByCode(String parentCode, String code) {
        Optional<EmdnNomenclature> parentFamily = repository.findByCode(parentCode);
        if (parentFamily.isPresent()) {
            return parentFamily.get().getSubtypes().stream()
                    .filter(subtype -> subtype.getCode().equals(code))
                    .findFirst();
        }
        return Optional.empty();
    }
     //3 eme niveaux
    // Récupérer un sous-type spécifique à partir d'une sous-famille (code: Z1101)
    public Optional<EmdnNomenclature> getSubtypeByCode(String parentCode, String subfamilyCode, String code) {
        Optional<EmdnNomenclature> parentFamily = repository.findByCode(parentCode);
        if (parentFamily.isPresent()) {
            Optional<EmdnNomenclature> subfamily = getSubfamilyByCode(parentCode, subfamilyCode);
            if (subfamily.isPresent()) {
                return subfamily.get().getSubtypes().stream()
                        .filter(subtype -> subtype.getCode().equals(code))
                        .findFirst();
            }
        }
        return Optional.empty();
    }
    //4eme niveaux
    // Récupérer un sous-type spécifique à partir d'une sous-sous-famille
    public Optional<EmdnNomenclature> getSubsubtypeByCode(String parentCode, String subfamilyCode, String subsubfamilyCode, String code) {
        Optional<EmdnNomenclature> parentFamily = repository.findByCode(parentCode);
        if (parentFamily.isPresent()) {
            // Récupérer la sous-famille
            Optional<EmdnNomenclature> subfamily = getSubfamilyByCode(parentCode, subfamilyCode);
            if (subfamily.isPresent()) {
                // Récupérer la sous-sous-famille
                Optional<EmdnNomenclature> subsubfamily = getSubfamilyByCode(subfamily.get().getCode(), subsubfamilyCode);
                if (subsubfamily.isPresent()) {
                    // Retourner le sous-type dans la sous-sous-famille
                    return subsubfamily.get().getSubtypes().stream()
                            .filter(subtype -> subtype.getCode().equals(code))
                            .findFirst();
                }
            }
        }
        return Optional.empty();
    }




    // Méthode générique pour récupérer les sous-types à différents niveaux
    public List<EmdnNomenclature> getSubtypesByParentCode(String parentCode) {
        Optional<EmdnNomenclature> parent = repository.findByCode(parentCode);
        return parent.map(EmdnNomenclature::getSubtypes).orElse(List.of());
    }


 */
}