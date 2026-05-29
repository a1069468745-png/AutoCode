package com.autocode.llm.web;

import com.autocode.llm.domain.LlmModelProfile;
import com.autocode.llm.domain.LlmModelProfileRepository;
import com.autocode.llm.web.dto.ModelProfileResponse;
import com.autocode.llm.web.dto.UpsertModelProfileRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/llm")
public class LlmModelController {

    private final LlmModelProfileRepository repository;

    public LlmModelController(LlmModelProfileRepository repository) {
        this.repository = repository;
    }

    @PutMapping("/projects/{projectId}/model-profile")
    public ModelProfileResponse upsertModelProfile(
            @PathVariable long projectId,
            @Valid @RequestBody UpsertModelProfileRequest request) {
        LlmModelProfile profile = repository.upsert(
                projectId,
                request.provider(),
                request.baseUrl(),
                request.modelName(),
                request.embeddingModel(),
                request.timeoutSeconds(),
                request.fallbackModel(),
                request.enableLocalOnly()
        );
        return ModelProfileResponse.from(profile);
    }

    @GetMapping("/projects/{projectId}/model-profile")
    public ModelProfileResponse getModelProfile(@PathVariable long projectId) {
        LlmModelProfile profile = repository.findByProjectId(projectId)
                .orElseThrow(() -> new ModelProfileNotFoundException(projectId));
        return ModelProfileResponse.from(profile);
    }

    @GetMapping("/model-profiles")
    public List<ModelProfileResponse> listModelProfiles() {
        return repository.findAll().stream()
                .map(ModelProfileResponse::from)
                .toList();
    }

    @DeleteMapping("/projects/{projectId}/model-profile")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteModelProfile(@PathVariable long projectId) {
        repository.deleteByProjectId(projectId);
    }
}
