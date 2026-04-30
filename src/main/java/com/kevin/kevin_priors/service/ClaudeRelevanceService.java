package com.kevin.kevin_priors.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevin.kevin_priors.dto.Case;
import com.kevin.kevin_priors.dto.Prediction;
import com.kevin.kevin_priors.dto.Study;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Calls Claude to figure out which prior studies are relevant.
 * If anything goes wrong we just say everything is not relevant.
 */
@Slf4j
@Primary
@Service
public class ClaudeRelevanceService implements RelevanceService {

    // TODO: maybe pull these out into application.properties later
    private static final int MAX_TOKENS = 16384;

    private AnthropicClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @PostConstruct
    void init() {
        // SDK reads ANTHROPIC_API_KEY from env
        this.client = AnthropicOkHttpClient.fromEnv();
        log.info("Claude client ready");
    }

    @Override
    public List<Prediction> predictForCase(Case c) {
        if (c.getPriorStudies() == null || c.getPriorStudies().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            String prompt = buildPrompt(c);
            // log.info("prompt: {}", prompt);  // useful when debugging
            String reply = askClaude(prompt);
            return parseClaudeReply(c, reply);
        } catch (Exception e) {
            log.warn("Claude failed for case {}: {}", c.getCaseId(), e.getMessage());
            return allFalse(c);
            // TODO: should retry once before giving up
        }
    }

    private String buildPrompt(Case c) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are helping a radiologist decide which prior exams are worth reviewing.\n\n");

        Study current = c.getCurrentStudy();
        sb.append("Current exam: ")
                .append(current.getStudyDescription())
                .append(" (").append(current.getStudyDate()).append(")\n\n");

        sb.append("Prior exams:\n");
        for (Study prior : c.getPriorStudies()) {
            sb.append("- study_id=").append(prior.getStudyId())
                    .append(", ").append(prior.getStudyDescription())
                    .append(" (").append(prior.getStudyDate()).append(")\n");
        }

        sb.append("\nFor each prior, return whether it is relevant ");
        sb.append("(same body region or clinically related).\n");
        sb.append("Reply with ONLY a JSON array, like:\n");
        sb.append("[{\"study_id\": \"...\", \"is_relevant\": true}, ...]");

        return sb.toString();
    }

    private String askClaude(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5)
                .maxTokens(MAX_TOKENS)
                .addUserMessage(prompt)
                .build();

        Message response = client.messages().create(params);
        return response.content().get(0).text().orElseThrow().text();
    }

    private List<Prediction> parseClaudeReply(Case c, String text) throws Exception {
        // Claude sometimes wraps the JSON in ```json ... ``` even when I ask it not to
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(json)?", "").replaceAll("```$", "").trim();
        }

        JsonNode arr = mapper.readTree(cleaned);
        List<Prediction> out = new ArrayList<>();

        for (JsonNode item : arr) {
            String sid = item.get("study_id").asText();
            boolean rel = item.get("is_relevant").asBoolean();
            out.add(new Prediction(c.getCaseId(), sid, rel));
        }

        // sometimes Claude drops a prior, fill those in as false
        if (out.size() != c.getPriorStudies().size()) {
            log.warn("got {} predictions for {} priors in case {}",
                    out.size(), c.getPriorStudies().size(), c.getCaseId());
            return fillGaps(c, out);
        }

        return out;
    }

    private List<Prediction> fillGaps(Case c, List<Prediction> partial) {
        List<Prediction> full = new ArrayList<>();
        for (Study prior : c.getPriorStudies()) {
            Prediction found = null;
            for (Prediction p : partial) {
                if (p.getStudyId().equals(prior.getStudyId())) {
                    found = p;
                    break;
                }
            }
            if (found == null) {
                found = new Prediction(c.getCaseId(), prior.getStudyId(), false);
            }
            full.add(found);
        }
        return full;
    }

    private List<Prediction> allFalse(Case c) {
        List<Prediction> result = new ArrayList<>();
        for (Study prior : c.getPriorStudies()) {
            result.add(new Prediction(c.getCaseId(), prior.getStudyId(), false));
        }
        return result;
    }
}
