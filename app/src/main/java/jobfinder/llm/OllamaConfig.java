package jobfinder.llm;

import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
class OllamaConfig {

    /** Ollama Cloud needs "Authorization: Bearer <key>"; Spring AI's Ollama properties have no api-key (checked on 2.0.1). */
    @Bean
    OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl,
                        @Value("${OLLAMA_API_KEY:}") String key) {
        RestClient.Builder rc = RestClient.builder();
        if (!key.isBlank()) rc.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key);
        return OllamaApi.builder().baseUrl(baseUrl).restClientBuilder(rc).build();
    }
}
