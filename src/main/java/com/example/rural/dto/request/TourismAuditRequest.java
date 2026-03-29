package com.example.rural.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员审核旅游项目请求
 *
 * 前端调用: PUT /api/v1/admin/tourism/spots/{id}/audit
 * Body: { "approved": true }                 → 通过
 *   或: { "approved": false, "reason": "..." } → 拒绝
 */
@Data
public class TourismAuditRequest {

    @NotNull(message = "请选择通过或拒绝")
    private Boolean approved;

    /** 拒绝理由（拒绝时必填） */
    private String reason;
}
