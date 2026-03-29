package com.example.rural.service;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.RouteCreateRequest;
import com.example.rural.dto.response.RouteResponse;

public interface RouteService {

    /** 创建路线 */
    RouteResponse createRoute(Long creatorId, RouteCreateRequest request);

    /** 编辑路线（整体替换行程项） */
    RouteResponse updateRoute(Long routeId, Long currentUserId, RouteCreateRequest request);

    /** 删除路线 */
    void deleteRoute(Long routeId, Long currentUserId);

    /** 路线详情 */
    RouteResponse getRouteDetail(Long routeId, Long currentUserId);

    /** 我的路线 */
    PageResult<RouteResponse> getMyRoutes(Long creatorId, int page, int size);

    /** 公开推荐路线 */
    PageResult<RouteResponse> getPublicRoutes(int page, int size);

    /** 复制别人的公开路线到我的（引用+1） */
    RouteResponse copyRoute(Long routeId, Long currentUserId);
}
