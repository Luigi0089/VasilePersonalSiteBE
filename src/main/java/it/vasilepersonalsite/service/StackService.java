package it.vasilepersonalsite.service;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;


import java.util.List;

public interface StackService {

    List<ProgettoDTO> getProgetti();

    ReadmeDTO getReadme(String repoName);
}
