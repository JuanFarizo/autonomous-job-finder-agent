package jobfinder.source;

import java.time.LocalDate;
import java.util.List;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

public final class JobMapper {

    private static final JsonMapper MAPPER = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();

    private JobMapper() {}

    public static List<Job> toJobs(String source, String rawJson) {
        List<RawJob> raws = MAPPER.readValue(rawJson, new TypeReference<List<RawJob>>() {});
        return raws.stream().map(r -> new Job(source, r.id(), r.jobUrl(), r.title(), r.company(), r.location(),
                parseDate(r.datePosted()), r.isRemote(), r.jobType(), r.jobLevel(), r.description())).toList();
    }

    private static LocalDate parseDate(String s) {
        return s == null || s.length() < 10 ? null : LocalDate.parse(s.substring(0, 10));
    }
}
