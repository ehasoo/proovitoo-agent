package ee.smit.agent.tool;

import ee.smit.agent.model.SourceCitation;
import ee.smit.agent.service.KnowledgeBaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseToolsTest {

    private KnowledgeBaseTools tools;

    @BeforeEach
    void setUp() {
        KnowledgeBaseService service = new KnowledgeBaseService();
        service.init();
        tools = new KnowledgeBaseTools(service);
    }

    @Test
    void testListTopics() {
        List<KnowledgeBaseTools.TopicSummary> topics = tools.listAllTopics();
        assertTrue(topics.size() >= 5);
        assertTrue(topics.stream().anyMatch(t -> "gitlab-access.md".equals(t.file())));
    }

    @Test
    void testSearchKnowledgeBase() {
        List<SourceCitation> results = tools.search("gitlab");
        assertFalse(results.isEmpty());
        assertEquals("gitlab-access.md", results.get(0).file());
    }

    @Test
    void testGetTopicContent() {
        String content = tools.readContent("gitlab-access.md");
        assertTrue(content.contains("GitLab ligipääsu taotlemine"));

        String nonExistent = tools.readContent("non-existent.md");
        assertTrue(nonExistent.startsWith("VIGA:"));
    }
}
