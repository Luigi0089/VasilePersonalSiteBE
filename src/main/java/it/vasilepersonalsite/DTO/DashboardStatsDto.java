package it.vasilepersonalsite.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardStatsDto {

    private long skills;
    private long categories;
    private long keywords;
    private long lezioniConfermate;
    private long lezioniDaConfermare;
}