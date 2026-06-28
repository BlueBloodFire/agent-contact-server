package cn.wjagent.ai.trigger.http;

import cn.wjagent.ai.api.response.Response;
import cn.wjagent.ai.domain.agent.service.IRagService;
import cn.wjagent.ai.types.enums.ResponseCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/rag/")
@CrossOrigin(origins = "*")
public class RagController {

    @Resource
    private IRagService ragService;

    @PostMapping("upload")
    public Response<Void> upload(
            @RequestParam String agentId,
            @RequestParam MultipartFile file) {
        try {
            log.info("RAG 上传文档 agentId:{} filename:{}", agentId, file.getOriginalFilename());
            ragService.uploadDocument(agentId, file.getOriginalFilename(), file.getBytes());
            return Response.<Void>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info("上传成功")
                    .build();
        } catch (Exception e) {
            log.error("RAG 上传文档失败", e);
            return Response.<Void>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info("上传失败：" + e.getMessage())
                    .build();
        }
    }

    @GetMapping("documents")
    public Response<List<String>> listDocuments(@RequestParam String agentId) {
        return Response.<List<String>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ragService.listDocuments(agentId))
                .build();
    }

    @GetMapping("search")
    public Response<List<String>> search(
            @RequestParam String agentId,
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK) {
        return Response.<List<String>>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(ragService.search(agentId, query, topK))
                .build();
    }
}
