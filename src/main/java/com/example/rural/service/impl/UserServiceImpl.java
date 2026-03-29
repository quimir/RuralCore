package com.example.rural.service.impl;

import com.example.rural.common.PageResult;
import com.example.rural.dto.request.*;
import com.example.rural.dto.response.LoginResponse;
import com.example.rural.dto.response.UserResponse;
import com.example.rural.entity.User;
import com.example.rural.exception.BusinessException;
import com.example.rural.repository.UserRepository;
import com.example.rural.service.UserService;
import com.example.rural.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户服务实现类
 *
 * ┌─────────────────────────────────────────────────────────┐
 * │  分层职责说明                                            │
 * │                                                         │
 * │  Controller  → 接收参数、调用Service、返回Result          │
 * │  Service     → 业务逻辑（本类）：校验、加密、权限判断      │
 * │  Repository  → 纯数据库操作：增删改查                     │
 * │  Entity      → 数据库表映射                              │
 * │  DTO         → 前后端数据传输（隔离Entity不暴露密码等）    │
 * └─────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 管理员重置密码时使用的默认密码 */
    private static final String DEFAULT_PASSWORD = "123456";

    // ======================== 注册 ========================

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的密码不一致");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已存在");
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()
                && userRepository.existsByPhone(request.getPhone())) {
            throw new BusinessException("该手机号已被注册");
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("该邮箱已被注册");
        }

        User.Role userRole = parseRole(request.getRole());

        // 金融服务商/保险提供商必须填机构信息
        if (userRole == User.Role.FINANCIAL_PROVIDER || userRole == User.Role.INSURANCE_PROVIDER) {
            if (request.getOrgName() == null || request.getOrgName().isBlank()) {
                throw new BusinessException("金融/保险机构注册必须填写机构名称");
            }
            if (request.getLicenseNo() == null || request.getLicenseNo().isBlank()) {
                throw new BusinessException("金融/保险机构注册必须填写经营许可编号");
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname() != null ? request.getNickname() : request.getUsername())
                .phone(request.getPhone())
                .email(request.getEmail())
                .role(userRole)
                .orgName(request.getOrgName())
                .licenseNo(request.getLicenseNo())
                .status(1)
                .build();

        User saved = userRepository.save(user);
        log.info("用户注册成功: username={}, role={}", saved.getUsername(), saved.getRole());
        return toUserResponse(saved);
    }

    // ======================== 登录 ========================

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        log.info("用户登录成功: username={}", user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .userInfo(LoginResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .nickname(user.getNickname())
                        .phone(user.getPhone())
                        .email(user.getEmail())
                        .avatar(user.getAvatar())
                        .role(user.getRole().name())
                        .build())
                .build();
    }

    // ======================== 个人信息 ========================

    @Override
    public UserResponse getUserById(Long userId) {
        return toUserResponse(findUserOrThrow(userId));
    }

    /**
     * 修改个人资料
     *
     * 只更新前端传过来的非空字段，其他字段保持不变。
     * 手机号和邮箱修改前要检查是否被其他用户占用。
     */
    @Override
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("该邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        User saved = userRepository.save(user);
        log.info("用户修改资料: userId={}", userId);
        return toUserResponse(saved);
    }

    /**
     * 修改密码
     *
     * 流程: 验证旧密码 → 校验新密码一致性 → BCrypt加密新密码 → 保存
     */
    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUserOrThrow(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        log.info("用户修改密码: userId={}", userId);
    }

    // ======================== 管理员操作 ========================

    /**
     * 分页查询用户列表（支持条件筛选）
     *
     * 使用 JPA Specification 动态拼接查询条件：
     *   keyword → 模糊匹配用户名或昵称
     *   role    → 精确匹配角色
     *
     * 前端请求示例: GET /api/v1/admin/users?keyword=张&role=FARMER&page=1&size=10
     */
    @Override
    public PageResult<UserResponse> listUsers(String keyword, String role, int page, int size) {
        PageRequest pageable = PageRequest.of(
                page - 1, size, Sort.by("createdAt").descending());

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                Predicate usernameLike = cb.like(root.get("username"), "%" + keyword + "%");
                Predicate nicknameLike = cb.like(root.get("nickname"), "%" + keyword + "%");
                predicates.add(cb.or(usernameLike, nicknameLike));
            }

            if (role != null && !role.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("role"), User.Role.valueOf(role.toUpperCase())));
                } catch (IllegalArgumentException ignored) { }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<User> userPage = userRepository.findAll(spec, pageable);
        return PageResult.from(userPage, this::toUserResponse);
    }

    /**
     * 管理员修改用户信息（角色、状态等）
     */
    @Override
    @Transactional
    public UserResponse adminUpdateUser(Long userId, AdminUpdateUserRequest request) {
        User user = findUserOrThrow(userId);

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BusinessException("该手机号已被其他用户使用");
            }
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("该邮箱已被其他用户使用");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getRole() != null) {
            try {
                user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("无效的角色类型: " + request.getRole());
            }
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException("状态值只能是 0(禁用) 或 1(正常)");
            }
            user.setStatus(request.getStatus());
        }

        User saved = userRepository.save(user);
        log.info("管理员修改用户: targetUserId={}", userId);
        return toUserResponse(saved);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserOrThrow(userId);
        if (user.getRole() == User.Role.ADMIN) {
            throw new BusinessException("不能删除管理员账号");
        }
        userRepository.deleteById(userId);
        log.info("管理员删除用户: userId={}", userId);
    }

    @Override
    @Transactional
    public void resetPassword(Long userId) {
        User user = findUserOrThrow(userId);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        userRepository.save(user);
        log.info("管理员重置密码: userId={}, 默认密码={}", userId, DEFAULT_PASSWORD);
    }

    // ======================== 工具方法 ========================

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(404, "用户不存在"));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .phone(user.getPhone())
                .email(user.getEmail())
                .avatar(user.getAvatar())
                .role(user.getRole().name())
                .status(user.getStatus())
                .orgName(user.getOrgName())
                .licenseNo(user.getLicenseNo())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private User.Role parseRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return User.Role.TOURIST;
        try {
            User.Role role = User.Role.valueOf(roleStr.toUpperCase());
            if (role == User.Role.ADMIN) throw new BusinessException("不允许注册为管理员角色");
            return role;
        } catch (IllegalArgumentException e) {
            throw new BusinessException("无效的角色类型: " + roleStr);
        }
    }
}
