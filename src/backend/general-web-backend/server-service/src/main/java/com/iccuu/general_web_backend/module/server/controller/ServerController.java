package com.iccuu.general_web_backend.module.server.controller;

import com.iccuu.general_web_backend.common.result.PageResult;
import jakarta.validation.Valid;
import com.iccuu.general_web_backend.common.result.R;
import jakarta.validation.Valid;
import com.iccuu.general_web_backend.module.server.entity.Server;
import jakarta.validation.Valid;
import com.iccuu.general_web_backend.module.server.service.ServerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/servers")
@RequiredArgsConstructor
public class ServerController {

    private final ServerService serverService;

    @GetMapping("/analytics")
    public R<Map<String, Object>> analytics() {
        return R.ok(serverService.getAnalytics());
    }

    @GetMapping
    public R<PageResult<Server>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        var mpPage = serverService.listServers(page, size);
        return R.ok(PageResult.of(mpPage.getTotal(), page, size, mpPage.getRecords()));
    }

    @PostMapping
    public R<Server> create(@Valid @RequestBody Server server) {
        return R.ok(serverService.create(server));
    }

    @PutMapping("/{id}")
    public R<Server> update(@PathVariable Long id, @Valid @RequestBody Server server) {
        return R.ok(serverService.update(id, server));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        serverService.delete(id);
        return R.ok();
    }

    @GetMapping("/{id}/collaborators")
    public R<Map<String, Object>> listCollaborators(@PathVariable Long id) {
        return R.ok(Map.of("collaborators", java.util.Collections.emptyList()));
    }

    @PostMapping("/{id}/collaborators")
    public R<Void> inviteCollaborator(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return R.ok();
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public R<Void> removeCollaborator(@PathVariable Long id, @PathVariable Long userId) {
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R<Map<String, Object>> testConnection(@PathVariable Long id) {
        return R.ok(serverService.testConnection(id));
    }
}
