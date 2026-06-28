package cn.wjagent.ai.domain.agent.service.chat;

import cn.wjagent.ai.domain.agent.service.IRagService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于关键词匹配的 RAG 服务（无需 embedding 模型，兼容所有 LLM 提供商）
 */
@Slf4j
@Service
public class RagService implements IRagService {

    // agentId -> List<chunk>
    private final Map<String, List<String>> chunkStore = new ConcurrentHashMap<>();
    // agentId -> List<filename>
    private final Map<String, List<String>> docRegistry = new ConcurrentHashMap<>();

    private static final int CHUNK_SIZE = 500;
    private static final int CHUNK_OVERLAP = 50;

    @Override
    public void uploadDocument(String agentId, String filename, byte[] content) {
        String text = new String(content, StandardCharsets.UTF_8);
        List<String> chunks = splitIntoChunks(text);
        chunkStore.computeIfAbsent(agentId, k -> new ArrayList<>()).addAll(chunks);
        docRegistry.computeIfAbsent(agentId, k -> new ArrayList<>()).add(filename);
        log.info("RAG 文档上传 agentId:{} filename:{} chunks:{}", agentId, filename, chunks.size());
    }

    @Override
    public List<String> search(String agentId, String query, int topK) {
        List<String> chunks = chunkStore.get(agentId);
        if (chunks == null || chunks.isEmpty()) return Collections.emptyList();

        String[] queryTerms = query.toLowerCase().split("\\s+");

        // Score each chunk by keyword match count
        List<Map.Entry<String, Integer>> scored = new ArrayList<>();
        for (String chunk : chunks) {
            String lower = chunk.toLowerCase();
            int score = 0;
            for (String term : queryTerms) {
                if (!term.isBlank() && lower.contains(term)) score++;
            }
            if (score > 0) scored.add(Map.entry(chunk, score));
        }

        scored.sort((a, b) -> b.getValue() - a.getValue());

        return scored.stream()
                .limit(topK)
                .map(Map.Entry::getKey)
                .toList();
    }

    @Override
    public List<String> listDocuments(String agentId) {
        return docRegistry.getOrDefault(agentId, Collections.emptyList());
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        int i = 0;
        while (i < len) {
            int end = Math.min(i + CHUNK_SIZE, len);
            chunks.add(text.substring(i, end));
            if (end == len) break;
            i += CHUNK_SIZE - CHUNK_OVERLAP;
        }
        return chunks;
    }
}
