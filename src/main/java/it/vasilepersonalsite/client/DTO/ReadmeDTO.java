package it.vasilepersonalsite.client.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ReadmeDTO {

    private String name;
    private String path;
    private String sha;
    private int size;
    private String url;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("git_url")
    private String gitUrl;

    @JsonProperty("download_url")
    private String downloadUrl;

    private String type;
    private String content;     // base64-encoded content of README
    private String encoding;    // typically "base64"
}
