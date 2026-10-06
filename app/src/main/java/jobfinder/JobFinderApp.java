package jobfinder;

import java.nio.file.Path;

import jobfinder.config.JsonFiles;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JobFinderApp {

    public static void main(String[] args) throws Exception {
        var config = JsonFiles.loadConfig(Path.of("../config/config.json"));
        var profile = JsonFiles.loadProfile(Path.of("../private/profile.json"));
        System.out.printf("Config: roles=%s seniority=%s max_age_days=%d%n", config.targetRoles(), config.seniority(), config.maxAgeDays());
        System.out.printf("Profile: %s, %d skills, %d proof points%n", profile.name(), profile.skills().size(), profile.proofPoints().size());
        SpringApplication.run(JobFinderApp.class, args);
    }
}
