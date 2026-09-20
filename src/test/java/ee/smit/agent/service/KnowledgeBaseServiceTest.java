package ee.smit.agent.service;

import ee.smit.agent.model.SourceCitation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeBaseServiceTest {

    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseService();
        knowledgeBaseService.init();
    }

    @Test
    void testAllTopicsLoaded() {
        List<KnowledgeBaseService.TopicDocument> allTopics = knowledgeBaseService.getAllTopics();
        assertTrue(allTopics.size() >= 5, "Knowledge base must have at least 5 topics, found: " + allTopics.size());

        List<String> fileNames = allTopics.stream().map(KnowledgeBaseService.TopicDocument::file).toList();
        assertTrue(fileNames.contains("gitlab-access.md"));
        assertTrue(fileNames.contains("kubernetes-deploy.md"));
        assertTrue(fileNames.contains("cicd-pipeline.md"));
        assertTrue(fileNames.contains("code-review.md"));
        assertTrue(fileNames.contains("vpn-access.md"));
    }

    @Test
    void testGetTopicSuccess() {
        Optional<KnowledgeBaseService.TopicDocument> doc = knowledgeBaseService.getTopic("gitlab-access.md");
        assertTrue(doc.isPresent());
        assertEquals("gitlab-access.md", doc.get().file());
        assertTrue(doc.get().content().contains("teenuste portaali"));
        assertFalse(doc.get().title().isBlank());
    }

    @Test
    void testPathTraversalRejection() {
        // SEC-06-U: Verify path traversal attempts are safely blocked
        Optional<KnowledgeBaseService.TopicDocument> traversal1 = knowledgeBaseService.getTopic("../../../etc/passwd");
        assertTrue(traversal1.isEmpty());

        Optional<KnowledgeBaseService.TopicDocument> traversal2 = knowledgeBaseService.getTopic("..\\..\\windows\\system32\\cmd.exe");
        assertTrue(traversal2.isEmpty());

        Optional<KnowledgeBaseService.TopicDocument> traversal3 = knowledgeBaseService.getTopic("/etc/shadow");
        assertTrue(traversal3.isEmpty());

        Optional<KnowledgeBaseService.TopicDocument> traversal4 = knowledgeBaseService.getTopic("secret/gitlab-access.md");
        assertTrue(traversal4.isEmpty());

        Optional<KnowledgeBaseService.TopicDocument> traversal5 = knowledgeBaseService.getTopic("   ");
        assertTrue(traversal5.isEmpty());

        Optional<KnowledgeBaseService.TopicDocument> traversal6 = knowledgeBaseService.getTopic(null);
        assertTrue(traversal6.isEmpty());
    }

    @Test
    void testSearchKnowledgeBase() {
        List<SourceCitation> results = knowledgeBaseService.search("gitlab ligipääs");
        assertFalse(results.isEmpty());
        assertEquals("gitlab-access.md", results.get(0).file());
        assertFalse(results.get(0).excerpt().isBlank());
    }

    @Test
    void testSearchKubernetes() {
        List<SourceCitation> results = knowledgeBaseService.search("kubernetes deploy");
        assertFalse(results.isEmpty());
        assertEquals("kubernetes-deploy.md", results.get(0).file());
    }

    @Test
    void testSearchEmptyQuery() {
        List<SourceCitation> results = knowledgeBaseService.search("");
        assertTrue(results.isEmpty());

        List<SourceCitation> nullResults = knowledgeBaseService.search(null);
        assertTrue(nullResults.isEmpty());
    }
}
