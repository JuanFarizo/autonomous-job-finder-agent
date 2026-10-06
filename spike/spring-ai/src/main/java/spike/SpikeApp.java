package spike;

import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SpikeApp {

    public static void main(String[] args) {
        org.springframework.boot.SpringApplication.run(SpikeApp.class, args);
    }

    /** Ollama Cloud needs "Authorization: Bearer <key>"; Spring AI's Ollama props have no api-key. */
    @Bean
    OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl,
                        @Value("${OLLAMA_API_KEY:}") String key) {
        RestClient.Builder rc = RestClient.builder();
        if (!key.isBlank()) rc.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        return OllamaApi.builder().baseUrl(baseUrl).restClientBuilder(rc).build();
    }

    @Bean
    ChatClient anthropicClient(AnthropicChatModel m) { return ChatClient.create(m); }

    @Bean
    ChatClient ollamaClient(OllamaChatModel m) { return ChatClient.create(m); }
}
