package it.vasilepersonalsite.controller;

import it.vasilepersonalsite.constans.ApiPath;
import it.vasilepersonalsite.service.LezioneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPath.BASE_PATH + "/" + ApiPath.ADMIN_PATH)
public class AdminController {

    @Autowired
    private LezioneService lezioniService;

    @GetMapping(value = "/conferma", produces = "text/plain")
    public ResponseEntity<String> confermaLezione(@RequestParam("id") String id){
       return ResponseEntity.ok(lezioniService.confermaLezione(id));
    }

    @GetMapping(value = "/rifiuta", produces = "text/plain")
    public ResponseEntity<String> rifiutaLezione(@RequestParam("id") String id){
        return ResponseEntity.ok(lezioniService.rifiutaLezione(id));
    }


    @GetMapping(value = "/posticipa", produces = "text/plain")
    public ResponseEntity<String> posticipaLezione(@RequestParam("id") String id){
        return ResponseEntity.ok(lezioniService.posticipaLezione(id));
    }
}
