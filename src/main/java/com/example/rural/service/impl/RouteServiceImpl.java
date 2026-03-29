package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.RouteCreateRequest;
import com.example.rural.dto.response.RouteResponse;
import com.example.rural.entity.TourismRoute;
import com.example.rural.entity.TourismRouteItem;
import com.example.rural.entity.TourismSpot;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.TourismRouteItemRepository;
import com.example.rural.repository.TourismRouteRepository;
import com.example.rural.repository.TourismSpotRepository;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.RouteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final TourismRouteRepository routeRepository;
    private final TourismRouteItemRepository routeItemRepository;
    private final TourismSpotRepository spotRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RouteResponse createRoute(Long creatorId, RouteCreateRequest request) {
        TourismRoute route = TourismRoute.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .creatorId(creatorId)
                .totalDays(request.getTotalDays() != null ? request.getTotalDays() : 1)
                .startDate(request.getStartDate())
                .isPublic(Boolean.TRUE.equals(request.getIsPublic()))
                .build();

        TourismRoute saved = routeRepository.save(route);

        // 保存行程项
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveRouteItems(saved.getId(), request.getItems());
        }

        // 设置封面为第一个景点的封面
        updateCoverImage(saved);

        log.info("路线创建: id={}, title={}, days={}", saved.getId(), saved.getTitle(), saved.getTotalDays());
        return toRouteResponse(saved);
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long routeId, Long currentUserId, RouteCreateRequest request) {
        TourismRoute route = findRouteOrThrow(routeId);
        checkOwner(route, currentUserId);

        route.setTitle(request.getTitle());
        route.setDescription(request.getDescription());
        if (request.getTotalDays() != null) route.setTotalDays(request.getTotalDays());
        route.setStartDate(request.getStartDate());
        if (request.getIsPublic() != null) route.setIsPublic(request.getIsPublic());

        // 整体替换行程项
        routeItemRepository.deleteByRouteId(routeId);
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            saveRouteItems(routeId, request.getItems());
        }

        TourismRoute saved = routeRepository.save(route);
        updateCoverImage(saved);
        return toRouteResponse(saved);
    }

    @Override
    @Transactional
    public void deleteRoute(Long routeId, Long currentUserId) {
        TourismRoute route = findRouteOrThrow(routeId);
        checkOwner(route, currentUserId);
        routeItemRepository.deleteByRouteId(routeId);
        routeRepository.delete(route);
    }

    @Override
    public RouteResponse getRouteDetail(Long routeId, Long currentUserId) {
        TourismRoute route = findRouteOrThrow(routeId);
        // 私有路线只有创建者可看
        if (!route.getIsPublic() && !route.getCreatorId().equals(currentUserId)) {
            throw new BusinessException(403, "无权查看此路线");
        }
        return toRouteResponse(route);
    }

    @Override
    public PageResult<RouteResponse> getMyRoutes(Long creatorId, int page, int size) {
        Page<TourismRoute> pageResult = routeRepository.findByCreatorIdOrderByUpdatedAtDesc(
                creatorId, PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toRouteResponse);
    }

    @Override
    public PageResult<RouteResponse> getPublicRoutes(int page, int size) {
        Page<TourismRoute> pageResult = routeRepository
                .findByIsPublicTrueOrderByCopyCountDescCreatedAtDesc(PageRequest.of(page - 1, size));
        return PageResult.from(pageResult, this::toRouteResponse);
    }

    /**
     * 复制公开路线到自己名下
     * 相当于「收藏并编辑」，原路线引用数 +1
     */
    @Override
    @Transactional
    public RouteResponse copyRoute(Long routeId, Long currentUserId) {
        TourismRoute original = findRouteOrThrow(routeId);
        if (!original.getIsPublic()) {
            throw new BusinessException("只能复制公开路线");
        }

        // 创建副本
        TourismRoute copy = TourismRoute.builder()
                .title(original.getTitle() + "（我的副本）")
                .description(original.getDescription())
                .creatorId(currentUserId)
                .totalDays(original.getTotalDays())
                .startDate(null)  // 用户自己设出发日期
                .isPublic(false)
                .coverImage(original.getCoverImage())
                .build();
        TourismRoute savedCopy = routeRepository.save(copy);

        // 复制行程项
        List<TourismRouteItem> originalItems =
                routeItemRepository.findByRouteIdOrderByDayNumberAscSortOrderAsc(routeId);
        for (TourismRouteItem item : originalItems) {
            TourismRouteItem copyItem = TourismRouteItem.builder()
                    .routeId(savedCopy.getId())
                    .spotId(item.getSpotId())
                    .dayNumber(item.getDayNumber())
                    .sortOrder(item.getSortOrder())
                    .visitTime(item.getVisitTime())
                    .durationMinutes(item.getDurationMinutes())
                    .notes(item.getNotes())
                    .build();
            routeItemRepository.save(copyItem);
        }

        // 原路线引用数 +1
        original.setCopyCount(original.getCopyCount() + 1);
        routeRepository.save(original);

        log.info("路线复制: originalId={}, copyId={}, user={}", routeId, savedCopy.getId(), currentUserId);
        return toRouteResponse(savedCopy);
    }

    // ==================== 工具方法 ====================

    private TourismRoute findRouteOrThrow(Long routeId) {
        return routeRepository.findById(routeId)
                .orElseThrow(() -> new BusinessException(404, "路线不存在"));
    }

    private void checkOwner(TourismRoute route, Long userId) {
        if (!route.getCreatorId().equals(userId)) {
            throw new BusinessException(403, "无权操作此路线");
        }
    }

    private void saveRouteItems(Long routeId, List<RouteCreateRequest.RouteItemDTO> items) {
        for (RouteCreateRequest.RouteItemDTO dto : items) {
            // 校验景点存在
            if (!spotRepository.existsById(dto.getSpotId())) {
                throw new BusinessException("景点ID " + dto.getSpotId() + " 不存在");
            }
            TourismRouteItem item = TourismRouteItem.builder()
                    .routeId(routeId)
                    .spotId(dto.getSpotId())
                    .dayNumber(dto.getDayNumber() != null ? dto.getDayNumber() : 1)
                    .sortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 1)
                    .visitTime(dto.getVisitTime())
                    .durationMinutes(dto.getDurationMinutes())
                    .notes(dto.getNotes())
                    .build();
            routeItemRepository.save(item);
        }
    }

    private void updateCoverImage(TourismRoute route) {
        List<TourismRouteItem> items =
                routeItemRepository.findByRouteIdOrderByDayNumberAscSortOrderAsc(route.getId());
        if (!items.isEmpty()) {
            spotRepository.findById(items.get(0).getSpotId()).ifPresent(spot -> {
                route.setCoverImage(spot.getCoverImage());
                routeRepository.save(route);
            });
        }
    }

    private RouteResponse toRouteResponse(TourismRoute route) {
        User creator = userRepository.findById(route.getCreatorId()).orElse(null);

        List<TourismRouteItem> items =
                routeItemRepository.findByRouteIdOrderByDayNumberAscSortOrderAsc(route.getId());

        // 按天分组
        Map<Integer, List<TourismRouteItem>> dayMap = items.stream()
                .collect(Collectors.groupingBy(TourismRouteItem::getDayNumber,
                        TreeMap::new, Collectors.toList()));

        List<RouteResponse.DayPlan> days = dayMap.entrySet().stream()
                .map(entry -> {
                    int dayNum = entry.getKey();
                    LocalDate date = route.getStartDate() != null
                            ? route.getStartDate().plusDays(dayNum - 1) : null;

                    List<RouteResponse.SpotItem> spotItems = entry.getValue().stream()
                            .map(item -> {
                                TourismSpot spot = spotRepository.findById(item.getSpotId()).orElse(null);
                                return RouteResponse.SpotItem.builder()
                                        .itemId(item.getId())
                                        .spotId(item.getSpotId())
                                        .spotName(spot != null ? spot.getTitle() : "[已删除]")
                                        .spotCoverImage(spot != null ? spot.getCoverImage() : null)
                                        .spotType(spot != null ? spot.getType().name() : null)
                                        .address(spot != null ? spot.getAddress() : null)
                                        .sortOrder(item.getSortOrder())
                                        .visitTime(item.getVisitTime())
                                        .durationMinutes(item.getDurationMinutes())
                                        .notes(item.getNotes())
                                        .build();
                            }).toList();

                    return RouteResponse.DayPlan.builder()
                            .dayNumber(dayNum)
                            .date(date)
                            .spots(spotItems)
                            .build();
                }).toList();

        return RouteResponse.builder()
                .id(route.getId())
                .title(route.getTitle())
                .description(route.getDescription())
                .totalDays(route.getTotalDays())
                .startDate(route.getStartDate())
                .isPublic(route.getIsPublic())
                .coverImage(route.getCoverImage())
                .copyCount(route.getCopyCount())
                .creatorId(route.getCreatorId())
                .creatorName(creator != null ? creator.getNickname() : "未知")
                .creatorAvatar(creator != null ? creator.getAvatar() : null)
                .days(days)
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }
}
