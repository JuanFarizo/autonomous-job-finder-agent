package jobfinder;

import java.nio.file.Path;
import java.util.List;

import jobfinder.config.JsonFiles;
import jobfinder.source.Collector;
import jobfinder.source.JobSpyLinkedInSource;
import jobfinder.source.RawStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobFinderApp {

    public static void main(String[] args) throws Exception {
        var config = JsonFiles.loadConfig(Path.of("../config/config.json"));
        var profile = JsonFiles.loadProfile(Path.of("../private/profile.json"));
        System.out.printf("Config: roles=%s seniority=%s max_age_days=%d%n", config.targetRoles(), config.seniority(), config.maxAgeDays());
        System.out.printf("Profile: %s, %d skills, %d proof points%n", profile.name(), profile.skills().size(), profile.proofPoints().size());
        if (List.of(args).contains("collect")) {
            var source = new JobSpyLinkedInSource(List.of("../sidecar/.venv/bin/python", "../sidecar/run_jobspy.py"));
            var collector = new Collector(source, new RawStore(Path.of("../data/raw")));
            var jobs = collector.collect(Collector.buildQueries(config, 10, false, null));
            System.out.printf("Collected %d jobs, raw saved in data/raw%n", jobs.size());
            return;
        }
        SpringApplication.run(JobFinderApp.class, args);
    }
}
