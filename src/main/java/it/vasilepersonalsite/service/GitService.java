package it.vasilepersonalsite.service;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

public interface GitService {

    List<ProgettoDTO> getProgetti(String username);

    ReadmeDTO getReadme(String username, String repoName);
}
