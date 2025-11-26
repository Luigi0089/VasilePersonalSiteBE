package it.vasilepersonalsite.service.impl;

import it.vasilepersonalsite.client.DTO.ProgettoDTO;
import it.vasilepersonalsite.client.DTO.ReadmeDTO;
import it.vasilepersonalsite.service.GitService;
import it.vasilepersonalsite.service.StackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StackServiceImpl implements StackService {
    /**
     * @return
     */

    @Autowired
    private GitService gitService;

    @Value("${git.username}")
    private String username;

    @Override
    public List<ProgettoDTO> getProgetti() {
        return gitService.getProgetti(username).stream()
                .sorted(Comparator.comparing(ProgettoDTO::getLanguage, Comparator.nullsLast(String::compareToIgnoreCase)))
                .collect(Collectors.toList());
    }

    /**
     * @param repoName
     * @return
     */
    @Override
    public ReadmeDTO getReadme(String repoName) {

        ReadmeDTO readmeDTO = gitService.getReadme(username, repoName);
        if (readmeDTO != null) {
            log.info("README recuperato con successo");
            if ("base64".equalsIgnoreCase(readmeDTO.getEncoding())) {

                String base64 = readmeDTO.getContent().replaceAll("\\s+", "");
                byte[] decoded = Base64.getDecoder().decode(base64);
                String markdown = new String(decoded, StandardCharsets.UTF_8);

                // riuso lo stesso DTO ma con il contenuto "umano"
                readmeDTO.setContent(markdown);
                readmeDTO.setEncoding("markdown"); // opzionale ma chiaro
            }
            return readmeDTO;
        }

        log.error("IL README NON è PRESENTE");
        return null;
    }

}

