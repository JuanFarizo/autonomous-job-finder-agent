package jobfinder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import jobfinder.config.JsonFiles;
import jobfinder.llm.OllamaScoreModel;
import jobfinder.obs.RunMetrics;
import jobfinder.pipeline.Pipeline;
import jobfinder.source.JobSpyLinkedInSource;
import jobfinder.source.RetryingSource;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/** Run from app/: `mvn spring-boot:run -Dspring-boot.run.arguments=run`. Without "run" it only loads config and profile. */
@SpringBootApplication
public class JobFinderApp {

    public static void main(String[] args) {
        System.exit(SpringApplication.exit(SpringApplication.run(JobFinderApp.class, args)));
    }

    @Bean
    ApplicationRunner runner(OllamaChatModel chat, @Value("${spring.ai.ollama.chat.model:}") String model) {
        return args -> {
            var config = JsonFiles.loadConfig(Path.of("../config/config.json"));
            var profile = JsonFiles.loadProfile(Path.of("../private/profile.json"));
            System.out.printf("Config: roles=%s seniority=%s max_age_days=%d%n", config.targetRoles(), config.seniority(), config.maxAgeDays());
            System.out.printf("Profile: %s, %d skills, %d proof points%n", profile.name(), profile.skills().size(), profile.proofPoints().size());
            if (!args.containsOption("run") && !args.getNonOptionArgs().contains("run")) return;
            if (model.isBlank()) throw new IllegalStateException("Set OLLAMA_MODEL (model not chosen yet, D16)");

            var source = RetryingSource.withRealSleep(
                    new JobSpyLinkedInSource(List.of("../sidecar/.venv/bin/python", "../sidecar/run_jobspy.py")), 2, 5_000);
            var result = new Pipeline(config, profile, source, new OllamaScoreModel(chat), Path.of("../data"), new RunMetrics())
                    .run(LocalDate.now());
            System.out.printf("Shortlist: %s (%d jobs), %d CV PDFs, metrics: %s%n",
                    result.shortlist(), result.shown().size(), result.cvCount(), result.metricsFile());
        };
    }
}
