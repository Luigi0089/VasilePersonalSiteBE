package it.vasilepersonalsite.client;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;
import it.vasilepersonalsite.config.FeignConfig;
import it.vasilepersonalsite.constans.GitConstans;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "GIT-SERVICE", url = GitConstans.BASE_URL, configuration = FeignConfig.class)
public interface GitClient {

    @RequestMapping(method = RequestMethod.GET, value = "/user/repos")
    ResponseEntity<List<ProgettoDTO>> getProgetti(
            @RequestParam("visibility") String visibility,
            @RequestParam("affiliation") String affiliation
    );

    @RequestMapping(method = RequestMethod.GET, value =  GitConstans.REPOS + "/{username}/" + "{repoName}/" + GitConstans.READEME)
    ResponseEntity<ReadmeDTO> getReadme(@PathVariable("username") String username, @PathVariable("repoName") String repoName);

}
