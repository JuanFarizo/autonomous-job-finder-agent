package spike;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class StructuredOutputTest {

    static final String PROMPT = "Score from 1 to 5 how well this job fits a senior Java developer: "
            + "'Senior Java/Spring Boot backend engineer, remote'. Answer briefly.";

    @Autowired @Qualifier("anthropicClient") ChatClient anthropic;
    @Autowired @Qualifier("ollamaClient") ChatClient ollama;

    @Test
    @EnabledIfEnvironmentVariable(named = "ANTHROPIC_API_KEY", matches = ".+")
    void anthropicStructured() {
        JobScore s = anthropic.prompt(PROMPT).call().entity(JobScore.class);
        System.out.println("ANTHROPIC -> " + s);
        assertThat(s).isNotNull();
        assertThat(s.score()).isBetween(1, 5);
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "OLLAMA_API_KEY", matches = ".+")
    void ollamaStructured() {
        JobScore s = ollama.prompt(PROMPT).call().entity(JobScore.class);
        System.out.println("OLLAMA -> " + s);
        assertThat(s).isNotNull();
        assertThat(s.score()).isBetween(1, 5);
    }
}
