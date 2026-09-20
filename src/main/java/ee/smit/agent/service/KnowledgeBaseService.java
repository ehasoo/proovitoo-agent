package ee.smit.agent.service;

import ee.smit.agent.model.SourceCitation;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+\\.md$");

    public record TopicDocument(
        String file,
        String title,
        String content,
        List<String> sections
    ) {}

    private volatile Map<String, TopicDocument> topics = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        loadKnowledgeBase();
    }

    public void loadKnowledgeBase() {
        Map<String, TopicDocument> newTopics = new ConcurrentHashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge-base/*.md");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null || !SAFE_FILENAME_PATTERN.matcher(filename).matches()) {
                    log.warn("Ignored non-conforming or unsafe knowledge base file: {}", filename);
                    continue;
                }

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                    String content = reader.lines().collect(Collectors.joining("\n"));
                    String title = extractTitle(content, filename);
                    List<String> sections = extractSections(content);

                    newTopics.put(filename.toLowerCase(Locale.ROOT), new TopicDocument(filename, title, content, sections));
                    log.info("Loaded knowledge base topic: {} ({})", filename, title);
                }
            }
            this.topics = newTopics;
        } catch (Exception e) {
            log.error("Failed to load knowledge base files", e);
        }
    }

    public List<TopicDocument> getAllTopics() {
        return new ArrayList<>(topics.values());
    }

    public Optional<TopicDocument> getTopic(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return Optional.empty();
        }

        String cleaned = fileName.trim();
        // Prevent path traversal attempts
        if (cleaned.contains("..") || cleaned.contains("/") || cleaned.contains("\\") || !SAFE_FILENAME_PATTERN.matcher(cleaned).matches()) {
            log.warn("Blocked potentially malicious path traversal request for filename: {}", fileName);
            return Optional.empty();
        }

        return Optional.ofNullable(topics.get(cleaned.toLowerCase(Locale.ROOT)));
    }

    private static final Set<String> STOP_WORDS = Set.of(
        "kuidas", "mis", "on", "kas", "kust", "see", "mul", "mida", "saab", "oma", "ning", "või", "ja", "kui", "ära", "enne", "pärast", "ole", "pole", "mulle", "sulle", "kõik", "the", "a", "an", "and", "or", "to", "for", "in", "of", "how", "do", "i"
    );

    public List<SourceCitation> search(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String[] tokens = query.toLowerCase(Locale.ROOT).split("[\\s,;:.?!+]+");
        List<String> keywords = Arrays.stream(tokens)
            .filter(t -> t.length() >= 3 && !STOP_WORDS.contains(t))
            .toList();

        if (keywords.isEmpty()) {
            return List.of();
        }

        List<ScoredCitation> scoredList = new ArrayList<>();

        for (TopicDocument doc : topics.values()) {
            String lowerContent = doc.content().toLowerCase(Locale.ROOT);
            String lowerTitle = doc.title().toLowerCase(Locale.ROOT);

            int score = 0;
            for (String kw : keywords) {
                if (lowerTitle.contains(kw)) {
                    score += 15;
                }
                if (lowerContent.contains(kw)) {
                    score += 3;
                }
            }

            if (score >= 5) {
                String excerpt = extractRelevantExcerpt(doc, keywords.toArray(new String[0]));
                scoredList.add(new ScoredCitation(new SourceCitation(doc.file(), doc.title(), excerpt), score));
            }
        }

        return scoredList.stream()
            .sorted(Comparator.comparingInt(ScoredCitation::score).reversed())
            .map(ScoredCitation::citation)
            .collect(Collectors.toList());
    }

    private record ScoredCitation(SourceCitation citation, int score) {}

    private String extractTitle(String content, String fallback) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                return trimmed.substring(2).trim();
            }
        }
        return fallback.replace(".md", "");
    }

    private List<String> extractSections(String content) {
        List<String> sections = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String line : content.split("\n")) {
            if (line.startsWith("## ") && !current.isEmpty()) {
                sections.add(current.toString().trim());
                current.setLength(0);
            }
            current.append(line).append("\n");
        }
        if (!current.isEmpty()) {
            sections.add(current.toString().trim());
        }
        return sections;
    }

    private String extractRelevantExcerpt(TopicDocument doc, String[] keywords) {
        // Try to find the section matching the most keywords
        String bestSection = "";
        int bestMatches = -1;

        for (String section : doc.sections()) {
            String lower = section.toLowerCase(Locale.ROOT);
            int matches = 0;
            for (String kw : keywords) {
                if (kw.length() > 2 && lower.contains(kw)) {
                    matches++;
                }
            }
            if (matches > bestMatches) {
                bestMatches = matches;
                bestSection = section;
            }
        }

        if (bestSection.isBlank()) {
            bestSection = doc.content();
        }

        // Limit excerpt length to ~300 chars
        String cleanExcerpt = bestSection.replaceAll("\\n+", " ").trim();
        if (cleanExcerpt.length() > 300) {
            return cleanExcerpt.substring(0, 297) + "...";
        }
        return cleanExcerpt;
    }
}
