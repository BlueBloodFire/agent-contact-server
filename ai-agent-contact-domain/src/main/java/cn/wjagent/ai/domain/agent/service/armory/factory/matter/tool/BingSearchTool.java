package cn.wjagent.ai.domain.agent.service.armory.factory.matter.tool;

import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("bingSearchTool")
public class BingSearchTool implements AiTool {

    private static final String TAVILY_ENDPOINT = "https://api.tavily.com/search";

    @Value("${tavily.search.api-key:}")
    private String apiKey;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Schema(description = "使用Tavily搜索互联网获取实时信息。当用户询问最新资讯、实时数据或你不确定的事实时调用此工具。返回搜索结果摘要。")
    public Map<String, Object> search(
            @Schema(name = "query", description = "搜索查询词，使用简洁的关键词") String query) {
        log.info("[TavilySearch] query={}", query);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[TavilySearch] api-key 未配置");
            return Map.of("error", "联网搜索未配置API Key");
        }
        try {
            String body = "{\"api_key\":\"" + apiKey + "\",\"query\":\"" + escapeJson(query) + "\",\"max_results\":5}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TAVILY_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("[TavilySearch] HTTP {} body={}", response.statusCode(), response.body());
                return Map.of("error", "搜索请求失败: HTTP " + response.statusCode());
            }
            String results = parseResults(response.body());
            log.info("[TavilySearch] 返回结果长度={}", results.length());
            return Map.of("results", results);
        } catch (Exception e) {
            log.error("[TavilySearch] 搜索异常", e);
            return Map.of("error", "搜索出错: " + e.getMessage());
        }
    }

    // Tavily 返回结构: {"results":[{"title":"...","url":"...","content":"..."},...]}
    private String parseResults(String json) {
        List<String> items = new ArrayList<>();
        int idx = json.indexOf("\"results\"");
        if (idx < 0) return "未找到相关搜索结果";
        int count = 0;
        while (count < 5) {
            int titleIdx = json.indexOf("\"title\":", idx);
            int urlIdx = json.indexOf("\"url\":", idx);
            int contentIdx = json.indexOf("\"content\":", idx);
            if (titleIdx < 0 || contentIdx < 0) break;

            String title = extractJsonString(json, titleIdx + 8);
            String url = urlIdx >= 0 && urlIdx < contentIdx ? extractJsonString(json, urlIdx + 6) : "";
            String content = extractJsonString(json, contentIdx + 10);

            items.add((count + 1) + ". 【" + title + "】\n   " + content + "\n   " + url);
            idx = contentIdx + 10 + content.length();
            count++;
        }
        return items.isEmpty() ? "未找到相关搜索结果" : String.join("\n\n", items);
    }

    private String extractJsonString(String json, int startIdx) {
        int from = json.indexOf('"', startIdx);
        if (from < 0) return "";
        StringBuilder sb = new StringBuilder();
        int i = from + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"' && json.charAt(i - 1) != '\\') break;
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == '"' || next == '\\') { sb.append(next); i += 2; continue; }
                if (next == 'n') { sb.append('\n'); i += 2; continue; }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    @Override
    public List<FunctionTool> getFunctionTools() {
        return List.of(FunctionTool.create(this, "search"));
    }
}
