package it.vasilepersonalsite.client.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ProgettoDTO {

    private Long id;

    private String name;

    @JsonProperty("html_url")
    private String htmlUrl;

    private String description;

    private String language;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("updated_at")
    private String updatedAt;

    @JsonProperty("stargazers_count")
    private int stargazersCount;

    @JsonProperty("forks_count")
    private int forksCount;
}