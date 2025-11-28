package it.vasilepersonalsite.controller;

import it.vasilepersonalsite.DTO.CategoryDto;
import it.vasilepersonalsite.DTO.KeywordDto;
import it.vasilepersonalsite.DTO.SkillDto;
import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.SkillService;
import it.vasilepersonalsite.service.StackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.STACK_PATH)
public class StackController {

    @Autowired
    private StackService stackService;

    @Autowired
    private SkillService skillService;

    @GetMapping("/progetti")
    public ResponseEntity<List<ProgettoDTO>> getProgetti() {
        return ResponseEntity.ok(stackService.getProgetti());
    }

    @GetMapping("/readme")
    public ResponseEntity<ReadmeDTO> getReadme(@RequestParam String repoName) {
        return ResponseEntity.ok(stackService.getReadme(repoName));
    }

// ========================
//       SKILL – PUBLIC
// ========================

    /**
     * Restituisce tutte le skill con categorie e keyword
     * (DTO già pronto per il FE).
     */
    @GetMapping("/skills")
    public ResponseEntity<List<SkillDto>> getAllSkills() {
        return ResponseEntity.ok(skillService.findAllSkills());
    }

    /**
     * Restituisce i dettagli di una singola skill.
     */
    @GetMapping("/skills/{id}")
    public ResponseEntity<SkillDto> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillService.findSkillById(id));
    }


// ========================
//   CATEGORY / KEYWORD – PUBLIC
// ========================

    /**
     * Restituisce tutte le categorie disponibili
     * (comodo per filtri nel FE).
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(skillService.findAllCategories());
    }

    /**
     * Restituisce tutte le keyword disponibili.
     */
    @GetMapping("/keywords")
    public ResponseEntity<List<KeywordDto>> getAllKeywords() {
        return ResponseEntity.ok(skillService.findAllKeywords());
    }


}
