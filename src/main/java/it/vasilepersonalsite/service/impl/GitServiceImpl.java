package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;
import it.vasilepersonalsite.client.GitClient;
import it.vasilepersonalsite.service.GitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
@Service
@Slf4j
public class GitServiceImpl implements GitService {

    /**
     * @param username
     * @return
     */

    @Autowired
    private GitClient gitClient;

    @Override
    public List<ProgettoDTO> getProgetti(String username) {
        log.info("getProgetti username={}", username);

        List<ProgettoDTO> progetti = gitClient.getProgetti(username).getBody();

        log.info("sono stati trovati {} progetti ", progetti != null ? progetti.size() : 0);

        return progetti;
    }

    /**
     * @param username
     * @param repoName
     * @return
     */
    @Override
    @Cacheable(cacheNames = "readme", key = "#repoName")
    public ReadmeDTO getReadme(String username, String repoName) {

        log.info("getReadme username={} repoName={}", username,repoName);

        ReadmeDTO readmeDTO = gitClient.getReadme(username,repoName).getBody();

            return readmeDTO;

    }

}
