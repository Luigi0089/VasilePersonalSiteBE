package it.vasilepersonalsite.controller;

import it.vasilepersonalsite.DTO.CategoryDto;
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
    @PostMapping("/skills")
    public ResponseEntity<SkillDto> createSkill(
            @RequestBody SkillDto dto,
            @RequestParam("password") String password
    ) {
        SkillDto created = skillService.createSkill(dto, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Aggiorna una skill esistente.
     */
    @PutMapping("/skills/{id}")
    public ResponseEntity<SkillDto> updateSkill(
            @PathVariable Long id,
            @RequestBody SkillDto dto,
            @RequestParam("password") String password
    ) {
        SkillDto updated = skillService.updateSkill(id, dto, password);
        return ResponseEntity.ok(updated);
    }

    /**
     * Elimina una skill esistente.
     */
    @DeleteMapping("/skills/{id}")
    public ResponseEntity<Void> deleteSkill(
            @PathVariable Long id,
            @RequestParam("password") String password
    ) {
        skillService.deleteSkill(id, password);
        return ResponseEntity.noContent().build();
    }

    // ========================
//     CATEGORY – ADMIN
// ========================

    @PostMapping("/skills/categories")
    public ResponseEntity<CategoryDto> createCategory(
            @RequestBody CategoryDto dto,
            @RequestParam("password") String password
    ) {
        CategoryDto created = skillService.createCategory(dto, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/skills/categories/{id}")
    public ResponseEntity<CategoryDto> updateCategory(
            @PathVariable Long id,
            @RequestBody CategoryDto dto,
            @RequestParam("password") String password
    ) {
        CategoryDto updated = skillService.updateCategory(id, dto, password);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/skills/categories/{id}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long id,
            @RequestParam("password") String password
    ) {
        skillService.deleteCategory(id, password);
        return ResponseEntity.noContent().build();
    }


    // ========================
//     KEYWORD – ADMIN
// ========================

    @PostMapping("/skills/keywords")
    public ResponseEntity<KeywordDto> createKeyword(
            @RequestBody KeywordDto dto,
            @RequestParam("password") String password
    ) {
        KeywordDto created = skillService.createKeyword(dto, password);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/skills/keywords/{id}")
    public ResponseEntity<KeywordDto> updateKeyword(
            @PathVariable Long id,
            @RequestBody KeywordDto dto,
            @RequestParam("password") String password
    ) {
        KeywordDto updated = skillService.updateKeyword(id, dto, password);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/skills/keywords/{id}")
    public ResponseEntity<Void> deleteKeyword(
            @PathVariable Long id,
            @RequestParam("password") String password
    ) {
        skillService.deleteKeyword(id, password);
        return ResponseEntity.noContent().build();
    }





}
