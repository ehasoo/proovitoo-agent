package ee.smit.agent.service;

import ee.smit.agent.model.AgentRequest;
import ee.smit.agent.model.AgentResponse;
import ee.smit.agent.model.ConfidenceLevel;
import ee.smit.agent.model.SourceCitation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);
    private static final Pattern CITATION_PATTERN = Pattern.compile("\\[allikas:\\s*([a-zA-Z0-9_-]+\\.md)\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern REFUSAL_INDICATORS = Pattern.compile(
        "(?i)(ei\\s+kuulu\\s+.*teadmusbaasi|ei\\s+saa\\s+aidata|ei\\s+ole\\s+IT-teenuste\\s+teadmusbaasis|keeldun|v[aä]ljaspool\\s+.*ulatust|pole\\s+teadmusbaasis|ei\\s+leitud\\s+teadmusbaasist)"
    );

    private final SecurityGuardrailService guardrailService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatClient chatClient;

    public AgentService(
        SecurityGuardrailService guardrailService,
        KnowledgeBaseService knowledgeBaseService,
        ChatClient chatClient
    ) {
        this.guardrailService = guardrailService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.chatClient = chatClient;
    }

    public AgentResponse ask(AgentRequest request) {
        String question = request.question();
        String sessionId = request.sessionId() != null && !request.sessionId().isBlank() 
            ? request.sessionId().trim() 
            : UUID.randomUUID().toString();

        // 1. Guardrail input validation
        SecurityGuardrailService.GuardrailDecision decision = guardrailService.validate(question, sessionId);
        if (!decision.allowed()) {
            if (decision.hardBadRequest()) {
                throw new IllegalArgumentException(decision.refusalReason());
            }
            return AgentResponse.refused(decision.refusalReason());
        }

        // 2. ChatClient Invocation
        String rawAnswer;
        try {
            rawAnswer = chatClient.prompt()
                .user(question)
                .advisors(advisorSpec -> advisorSpec.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .call()
                .content();
        } catch (Exception e) {
            log.error("OpenAI API or ChatClient execution failure", e);
            return AgentResponse.refused("Vabandust, päringu töötlemisel tekkis tehniline viga. Palun proovi mõne hetke pärast uuesti.");
        }

        if (rawAnswer == null || rawAnswer.isBlank()) {
            return AgentResponse.refused("Vastust ei õnnestunud genereerida.");
        }

        // 3. Post-generation inspection & citation extraction
        return processAndValidateResponse(rawAnswer, question);
    }

    public AgentResponse processAndValidateResponse(String rawAnswer, String originalQuestion) {
        // Check if model refused or stated topic is out-of-scope
        if (REFUSAL_INDICATORS.matcher(rawAnswer).find()) {
            return new AgentResponse(
                rawAnswer,
                List.of(),
                ConfidenceLevel.LOW,
                true,
                rawAnswer
            );
        }

        // Extract citations from the response
        Set<String> citedFiles = new LinkedHashSet<>();
        Matcher matcher = CITATION_PATTERN.matcher(rawAnswer);
        while (matcher.find()) {
            citedFiles.add(matcher.group(1).toLowerCase(Locale.ROOT));
        }

        // Also check if any known knowledge-base markdown files are referenced directly
        for (var topicDoc : knowledgeBaseService.getAllTopics()) {
            if (rawAnswer.toLowerCase(Locale.ROOT).contains(topicDoc.file().toLowerCase(Locale.ROOT))) {
                citedFiles.add(topicDoc.file().toLowerCase(Locale.ROOT));
            }
        }

        // If user specifically asked to list available topics and the agent provided a list of topics
        boolean isTopicListQuery = originalQuestion != null && 
            Pattern.compile("(?i)(teemad|saadaolevad|info[a-z]*\\s+anda|mis\\s+teemadel|millest|loetle)").matcher(originalQuestion).find();
        if (citedFiles.isEmpty() && isTopicListQuery && !REFUSAL_INDICATORS.matcher(rawAnswer).find()) {
            for (var topicDoc : knowledgeBaseService.getAllTopics()) {
                citedFiles.add(topicDoc.file().toLowerCase(Locale.ROOT));
            }
        }

        List<SourceCitation> sources = new ArrayList<>();
        for (String file : citedFiles) {
            knowledgeBaseService.getTopic(file).ifPresent(doc -> {
                String excerpt = doc.content().lines().limit(3).reduce("", (a, b) -> a + " " + b).trim();
                sources.add(new SourceCitation(doc.file(), doc.title(), excerpt));
            });
        }

        // Enforcement: If no valid source was cited, refuse the response
        if (sources.isEmpty()) {
            return new AgentResponse(
                null,
                List.of(),
                ConfidenceLevel.LOW,
                true,
                "Vastusele ei leitud usaldusväärset allikat teadmusbaasist."
            );
        }

        return new AgentResponse(
            rawAnswer,
            sources,
            ConfidenceLevel.HIGH,
            false,
            null
        );
    }
}
