package cn.wjagent.ai.domain.agent.service;

import java.util.List;

public interface IRagService {
    void uploadDocument(String agentId, String filename, byte[] content);
    List<String> search(String agentId, String query, int topK);
    List<String> listDocuments(String agentId);
}
