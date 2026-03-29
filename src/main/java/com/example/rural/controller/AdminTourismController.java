package com.example.rural.controller;

import com.example.rural.common.PageResult;
import com.example.rural.common.Result;
import com.example.rural.dto.request.TourismAuditRequest;
import com.example.rural.dto.response.TourismSpotResponse;
import com.example.rural.service.TourismService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理员 — 旅游项目审核管理
 *
 * 所有接口路径以 /api/v1/admin/ 开头，SecurityConfig 中已配置 hasRole("ADMIN")
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  GET  /api/v1/admin/tourism/spots           查看所有（含待审核）│
 * │  PUT  /api/v1/admin/tourism/spots/{id}/audit 审核（通过/拒绝） │
 * └──────────────────────────────────────────────────────────────┘
 */
@RestController
@RequestMapping("/api/v1/admin/tourism")
@RequiredArgsConstructor
public class AdminTourismController {

    private final TourismService tourismService;

    /**
     * 管理员查看所有旅游项目
     *
     * GET /api/v1/admin/tourism/spots?status=PENDING&page=1&size=10
     *
     * status 可选: PENDING / APPROVED / REJECTED / OFFLINE（不传则查全部）
     */
    @GetMapping("/spots")
    public Result<PageResult<TourismSpotResponse>> adminListSpots(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(tourismService.adminListSpots(status, page, size));
    }

    /**
     * 审核旅游项目
     *
     * PUT /api/v1/admin/tourism/spots/5/audit
     * Body: { "approved": true }                   → 通过
     *   或: { "approved": false, "reason": "..." }  → 拒绝（必须填理由）
     */
    @PutMapping("/spots/{id}/audit")
    public Result<TourismSpotResponse> auditSpot(@PathVariable Long id,
                                                  @Valid @RequestBody TourismAuditRequest request) {
        return Result.ok(tourismService.auditSpot(id, request));
    }
}
