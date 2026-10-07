package jobfinder.llm;

import java.util.List;

import jobfinder.score.ModelAnswer;
import jobfinder.score.ScoreModel;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

/** ScoreModel backed by Spring AI (Ollama Cloud). Reports provider token usage when present. UNVERIFIED live (no key yet). */
public class OllamaScoreModel implements ScoreModel {

    private final ChatModel chat;

    public OllamaScoreModel(ChatModel chat) {
        this.chat = chat;
    }

    @Override
    public ModelAnswer call(String systemPrompt, String userPrompt) {
        ChatResponse r = chat.call(new Prompt(List.of(new SystemMessage(systemPrompt), new UserMessage(userPrompt))));
        var usage = r.getMetadata() == null ? null : r.getMetadata().getUsage();
        Integer in = usage == null || usage.getPromptTokens() == null || usage.getPromptTokens() == 0 ? null : usage.getPromptTokens().intValue();
        Integer out = usage == null || usage.getCompletionTokens() == null || usage.getCompletionTokens() == 0 ? null : usage.getCompletionTokens().intValue();
        return new ModelAnswer(r.getResult().getOutput().getText(), in, out);
    }
}
