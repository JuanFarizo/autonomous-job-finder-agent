# Spike: Spring Boot 4 + Spring AI 2.x structured output (T4, Phase 0)

Date: 2026-10-06. Status: COMPILES, context wiring OK, LIVE CALLS NOT RUN (ANTHROPIC_API_KEY and OLLAMA_API_KEY not present in env).

## Environment found
- Java: OpenJDK Corretto 25.0.4 (sdkman, default; unchanged). Maven 3.9.10.
- Minimum Java: Spring AI 2.0 baseline Java 21+ (https://docs.spring.io/spring-ai/reference/upgrade-notes.html). Spring Boot 4 baseline Java 17 (not re-verified here). Project sets java.version=21, built on 25 OK.
- Non-interactive shell needs `source ~/.sdkman/bin/sdkman-init.sh` (zsh -ic printed gitstatus/zle noise but worked).

## Coordinates (resolved from Maven Central)
- Parent: org.springframework.boot:spring-boot-starter-parent:4.0.8 (Spring AI docs: 4.0.x and 4.1.x supported)
- BOM: org.springframework.ai:spring-ai-bom:2.0.1 (stable; 2.1.0-M1 is milestone). Central has 2.0.0, 2.0.1.
- org.springframework.ai:spring-ai-starter-model-anthropic (uses official com.anthropic anthropic-java SDK)
- org.springframework.ai:spring-ai-starter-model-ollama
- Docs: https://docs.spring.io/spring-ai/reference/getting-started.html ,
  https://docs.spring.io/spring-ai/reference/api/chat/anthropic-chat.html ,
  https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html ,
  https://docs.spring.io/spring-ai/reference/upgrade-notes.html ,
  https://docs.ollama.com/cloud

## Evidence
- `mvn -q compile`: no output, success.
- `env -u ANTHROPIC_API_KEY -u OLLAMA_API_KEY mvn verify`: `Tests run: 2, Failures: 0, Errors: 0, Skipped: 2`, BUILD SUCCESS. Spring context started (1.09 s) with both providers and the custom OllamaApi bean, so wiring is valid.
- `ChatClient.prompt(..).call().entity(JobScore.class)` compiles against 2.0.1 (record JobScore(int score, String reason)).
- Two live tests are skipped by @EnabledIfEnvironmentVariable when keys are absent (reported as Skipped, not passed). NO live call was made, so structured output from either provider is UNVERIFIED.

## Config (src/main/resources/application.properties)
- spring.ai.anthropic.api-key=${ANTHROPIC_API_KEY:missing}; chat.model=claude-sonnet-5-5; chat.max-tokens=300
- spring.ai.ollama.base-url=https://ollama.com; chat.model=${OLLAMA_MODEL:gpt-oss:120b}; options.num-predict=300
- Placeholder `missing` for Anthropic key avoids startup failure; a live call without a key would fail with auth error (not silent).

## Findings / unverified
1. Spring AI 2.0.1 Ollama has NO api-key property (OllamaConnectionProperties only has base-url; checked via javap). Ollama Cloud needs `Authorization: Bearer $OLLAMA_API_KEY` (docs.ollama.com/cloud), so SpikeApp defines a custom OllamaApi bean with a RestClient default header. Context starts, but the header actually authenticating against ollama.com is UNVERIFIED.
2. Ollama Cloud base URL https://ollama.com (API path /api/chat) per docs.ollama.com/cloud; Spring's OllamaApi appends /api/chat, assumed compatible, UNVERIFIED. Alternative: OpenAI-compatible endpoint with spring-ai-starter-model-openai (not tried).
3. Ollama cloud model name `gpt-oss:120b` is a guess (docs say API names differ from CLI names, e.g. gemma4:31b vs gemma4:cloud). Free-tier limits and which models are free: UNVERIFIED. Override with OLLAMA_MODEL.
4. Spring AI Anthropic docs do not mention `claude-sonnet-5-5` (they list older ids). Whether the id works is UNVERIFIED until a live call. Anthropic structured output via entity() in Spring AI uses prompt-format instructions by default; native structured output option exists per docs (mentioned with claude-sonnet-4-6), not tested.
5. Two ChatModel beans exist, so the autoconfigured ChatClient.Builder is ambiguous; clients are built manually with ChatClient.create(model).
6. Spring AI 2.0 notes: Jackson 3 required; ChatClient options now take a builder; maxTokens default 4096 (we set 300).

## Contradictions with specs
- None blocking. Specs say "Spring Boot 4 + Spring AI 2.x": confirmed available (Boot 4.0.8 + AI 2.0.1). Note spec/decision D16 model id is not documented by Spring AI yet.

## Run live (owner)
    export ANTHROPIC_API_KEY=...   # never commit
    export OLLAMA_API_KEY=...
    source ~/.sdkman/bin/sdkman-init.sh && mvn test   # prints "ANTHROPIC -> JobScore[...]" / "OLLAMA -> ..."
Each test makes one tiny call.

## Files
pom.xml, src/main/java/spike/{SpikeApp,JobScore}.java, src/main/resources/application.properties, src/test/java/spike/StructuredOutputTest.java
