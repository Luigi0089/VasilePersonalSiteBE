package it.vasilepersonalsite.controller;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.StackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.STACK_PATH)
public class StackController {

    @Autowired
    private StackService stackService;

    @GetMapping("/progetti")
    public ResponseEntity<List<ProgettoDTO>> getProgetti() {
        return ResponseEntity.ok(stackService.getProgetti());
    }

    @GetMapping("/readme")
    public ResponseEntity<String> getReadme(@RequestParam String repoName) {
        return ResponseEntity.ok(stackService.getReadme(repoName));
    }

}
