package jobfinder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import jobfinder.config.JsonFiles;
import jobfinder.llm.OllamaScoreModel;
import jobfinder.obs.RunMetrics;
import jobfinder.pipeline.Pipeline;
import jobfinder.source.JobSource;
import jobfinder.source.JobSpySource;
import jobfinder.source.RetryingSource;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Run from app/: `mvn spring-boot:run -Dspring-boot.run.arguments=run`. Without
 * "run" it only loads config and profile.
 */
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
            System.out.printf("Config: roles=%s seniority=%s max_age_days=%d%n", config.targetRoles(),
                    config.seniority(), config.maxAgeDays());
            System.out.printf("Profile: %s, %d skills, %d proof points%n", profile.name(), profile.skills().size(),
                    profile.proofPoints().size());
            if (args.containsOption("tailor")) { // ./scripts/tailor.sh job.txt: grounded CV for one pasted posting
                if (args.containsOption("profile")) // hand-curated profile for this one posting (wording approved by the owner)
                    profile = JsonFiles.loadProfile(Path.of(args.getOptionValues("profile").get(0)));
                Path f = Path.of(args.getOptionValues("tailor").get(0));
                String text = java.nio.file.Files.readString(f);
                String stem = f.getFileName().toString().replaceAll("\\.[^.]*$", "");
                var job = new jobfinder.source.Job("manual", stem, null, text.lines().findFirst().orElse(""), "", "", null,
                        null, null, null, text);
                Path out = Path.of("../data/cv/manual_" + stem + ".pdf");
                jobfinder.tailor.CvPdf.write(jobfinder.tailor.Tailorer.tailor(job, profile), profile, out);
                System.out.println("CV: " + out.toAbsolutePath().normalize());
                return;
            }
            if (!args.containsOption("run") && !args.getNonOptionArgs().contains("run"))
                return;
            if (model.isBlank())
                throw new IllegalStateException("Set OLLAMA_MODEL (model not chosen yet, D16)");

            List<JobSource> sources = config.sources().stream()
                    .<JobSource>map(site -> RetryingSource.withRealSleep(new JobSpySource(site,
                            List.of("../sidecar/.venv/bin/python", "../sidecar/run_jobspy.py")), 2, 5_000))
                    .toList();
            var result = new Pipeline(config, profile, sources, new OllamaScoreModel(chat), Path.of("../data"),
                    new RunMetrics())
                    .run(LocalDate.now());
            System.out.printf("Shortlist: %s (%d jobs), %d CV PDFs, metrics: %s%n",
                    result.shortlist(), result.shown().size(), result.cvCount(), result.metricsFile());
        };
    }
}
