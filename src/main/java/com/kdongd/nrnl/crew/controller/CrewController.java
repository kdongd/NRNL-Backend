package com.kdongd.nrnl.crew.controller;

import com.kdongd.nrnl.crew.domain.CrewStatus;
import com.kdongd.nrnl.crew.dto.CrewApplicationResponse;
import com.kdongd.nrnl.crew.dto.CrewCreateRequest;
import com.kdongd.nrnl.crew.dto.CrewLeadershipTransferRequest;
import com.kdongd.nrnl.crew.dto.CrewMemberResponse;
import com.kdongd.nrnl.crew.dto.CrewResponse;
import com.kdongd.nrnl.crew.dto.CrewUpdateRequest;
import com.kdongd.nrnl.crew.service.CrewService;
import com.kdongd.nrnl.domain.WorkoutType;
import com.kdongd.nrnl.dto.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Tag(name = "Crew", description = "크루 및 가입 신청 관리 API")
@RestController
@RequestMapping("/crews")
public class CrewController {

    private final CrewService service;

    public CrewController(CrewService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<CrewResponse> search(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) WorkoutType sport,
            @RequestParam(required = false) CrewStatus status,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return PageResponse.from(service.search(name, sport, status, pageable));
    }

    @GetMapping("/{id}")
    public CrewResponse detail(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/members")
    public List<CrewMemberResponse> members(@PathVariable Long id) {
        return service.findMembers(id);
    }

    @PostMapping
    public ResponseEntity<CrewResponse> create(
            @Valid @RequestBody CrewCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        CrewResponse saved = service.create(request, userId);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.id())
                .toUri();
        return ResponseEntity.created(location).body(saved);
    }

    @PutMapping("/{id}")
    public CrewResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CrewUpdateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return service.update(id, request, userId);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        service.delete(id, userId);
    }

    @PostMapping("/{id}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    public CrewApplicationResponse apply(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return service.apply(id, userId);
    }

    @GetMapping("/{id}/applications")
    public PageResponse<CrewApplicationResponse> applications(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId,
            @ParameterObject @PageableDefault(size = 10) Pageable pageable
    ) {
        return PageResponse.from(service.findApplications(id, userId, pageable));
    }

    @PostMapping("/{id}/applications/{applicationId}/cancel")
    public CrewApplicationResponse cancel(
            @PathVariable Long id,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Long userId
    ) {
        return service.cancel(id, applicationId, userId);
    }

    @PostMapping("/{id}/applications/{applicationId}/approve")
    public CrewApplicationResponse approve(
            @PathVariable Long id,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Long userId
    ) {
        return service.approve(id, applicationId, userId);
    }

    @PostMapping("/{id}/applications/{applicationId}/reject")
    public CrewApplicationResponse reject(
            @PathVariable Long id,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal Long userId
    ) {
        return service.reject(id, applicationId, userId);
    }

    @PutMapping("/{id}/leader")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferLeadership(
            @PathVariable Long id,
            @Valid @RequestBody CrewLeadershipTransferRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        service.transferLeadership(id, userId, request.userId());
    }

    @DeleteMapping("/{id}/members/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leave(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        service.leave(id, userId);
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void expel(
            @PathVariable Long id,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal Long userId
    ) {
        service.expel(id, userId, targetUserId);
    }
}
