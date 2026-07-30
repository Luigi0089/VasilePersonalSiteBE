package it.vasilepersonalsite.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.vasilepersonalsite.DTO.CategoryDto;
import it.vasilepersonalsite.DTO.DashboardStatsDto;
import it.vasilepersonalsite.DTO.KeywordDto;
import it.vasilepersonalsite.DTO.SkillDto;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.LezioneService;
import it.vasilepersonalsite.service.SkillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.ADMIN_PATH)
public class AdminController {

    @Autowired
    private LezioneService lezioniService;

    @Autowired
    private SkillService skillService;

    @GetMapping(value = "/conferma", produces = "text/plain")
    public ResponseEntity<String> confermaLezione(@RequestParam("id") String id){
       return ResponseEntity.ok(lezioniService.confermaLezione(id));
    }

    @GetMapping(value = "/rifiuta", produces = "text/plain")
    public ResponseEntity<String> rifiutaLezione(@RequestParam("id") String id){
        return ResponseEntity.ok(lezioniService.rifiutaLezione(id));
    }

    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        log.info("PING");
        return ResponseEntity.ok("OK");
    }


    // ========================
//       SKILL – ADMIN
// ========================

    /**
     * Crea una nuova skill.
     * Richiede password admin come query param (?password=...).
     */
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/skills")
    public ResponseEntity<SkillDto> createSkill(
            @RequestBody SkillDto dto
    ) {
        SkillDto created = skillService.createSkill(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Aggiorna una skill esistente.
     */
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/skills/{id}")
    public ResponseEntity<SkillDto> updateSkill(
            @PathVariable Long id,
            @RequestBody SkillDto dto
    ) {
        SkillDto updated = skillService.updateSkill(id, dto);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina una skill esistente.
     */
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long id
    ) {
        skillService.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }

    // ========================
//     CATEGORY – ADMIN
// ========================

    @PostMapping("/skills/categories")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CategoryDto> createCategory(
            @RequestBody CategoryDto dto
    ) {
        CategoryDto created = skillService.createCategory(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/skills/categories/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto dto
    ) {
        CategoryDto updated = skillService.updateCategory(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/skills/categories/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id
    ) {
        skillService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }


    // ========================
//     KEYWORD – ADMIN
// ========================

    @PostMapping("/skills/keywords")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<KeywordDto> createKeyword(
            @RequestBody KeywordDto dto
    ) {
        KeywordDto created = skillService.createKeyword(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/skills/keywords/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<KeywordDto> updateKeyword(
            @PathVariable Long id,
            @RequestBody KeywordDto dto
    ) {
        KeywordDto updated = skillService.updateKeyword(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/skills/keywords/{id}")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteKeyword(
            @PathVariable Long id
    ) {
        skillService.deleteKeyword(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/dashboard/stats")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<DashboardStatsDto> getDashboardStatistics(
            @RequestParam("data")
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            LocalDate data
    ) {

        DashboardStatsDto statistics = new DashboardStatsDto(
                skillService.countSkills(),
                skillService.countCategories(),
                skillService.countKeywords(),
                lezioniService.countLezioniConfermateSettimana(data),
                lezioniService.countLezioniDaConfermareSettimana(data)
        );


        return ResponseEntity.ok(statistics);
    }


}
