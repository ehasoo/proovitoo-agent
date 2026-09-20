package ee.smit.agent.tool;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import ee.smit.agent.model.SourceCitation;
import ee.smit.agent.service.KnowledgeBaseService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.List;
import java.util.function.Function;

@Configuration
public class KnowledgeBaseTools {

    public record SearchRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Otsingusõnad või teema, nt 'gitlab', 'deploy', 'vpn'")
        String query
    ) {}

    public record ListTopicsRequest() {}

    public record TopicContentRequest(
        @JsonProperty(required = true)
        @JsonPropertyDescription("Failinimi teadmusbaasis, nt 'gitlab-access.md'")
        String fileName
    ) {}

    public record TopicSummary(
        String file,
        String title
    ) {}

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseTools(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Bean
    @Description("Otsib IT siseveebi teadmusbaasist märksõnade või küsimuse põhjal asjakohaseid allikaid ja väljavõtteid.")
    public Function<SearchRequest, List<SourceCitation>> searchKnowledgeBase() {
        return request -> knowledgeBaseService.search(request != null ? request.query() : "");
    }

    @Bean
    @Description("Loetleb kõik teadmusbaasis saadaolevad IT teemad, nende failinimed ja pealkirjad.")
    public Function<ListTopicsRequest, List<TopicSummary>> listTopics() {
        return request -> knowledgeBaseService.getAllTopics().stream()
            .map(doc -> new TopicSummary(doc.file(), doc.title()))
            .toList();
    }

    @Bean
    @Description("Tagastab konkreetse teadmusbaasi faili täieliku sisu failinime järgi (nt 'gitlab-access.md').")
    public Function<TopicContentRequest, String> getTopicContent() {
        return request -> {
            if (request == null || request.fileName() == null) {
                return "VIGA: Failinimi on kohustuslik.";
            }
            return knowledgeBaseService.getTopic(request.fileName())
                .map(KnowledgeBaseService.TopicDocument::content)
                .orElse("VIGA: Teemat või faili nimega '" + request.fileName() + "' ei leitud teadmusbaasist.");
        };
    }

    // Direct helper methods for programmatic invocations and unit tests
    public List<SourceCitation> search(String query) {
        return searchKnowledgeBase().apply(new SearchRequest(query));
    }

    public List<TopicSummary> listAllTopics() {
        return listTopics().apply(new ListTopicsRequest());
    }

    public String readContent(String fileName) {
        return getTopicContent().apply(new TopicContentRequest(fileName));
    }
}
