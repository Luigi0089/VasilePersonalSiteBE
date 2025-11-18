package it.vasilepersonalsite.service;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;


import java.util.List;

public interface StackService {

    List<ProgettoDTO> getProgetti();

    String getReadme(String repoName);
}
