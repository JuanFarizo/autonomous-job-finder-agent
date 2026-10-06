package jobfinder.config;

import java.util.List;

/** User-facing configuration, loaded from config/config.json. Null means "not decided by the owner". */
public record AppConfig(
        int maxAgeDays,
        List<String> sources,
        List<String> targetRoles,
        List<String> seniority,
        List<String> languages,
        LocationRules location,
        List<String> excludeKeywords,
        Double minScore,
        Integer resultsPerSearch) {

    /**
     * Accepted: located in Argentina, or remote open to Argentina, or remote from anywhere.
     * Rejected: remote restricted to another country or region.
     * onsiteArgentina is null until the owner decides.
     */
    public record LocationRules(
            String residence,
            boolean acceptRemoteWorldwide,
            boolean acceptRemoteInResidence,
            boolean rejectRemoteRestrictedToOtherRegions,
            Boolean acceptOnsiteInResidence) {}
}
