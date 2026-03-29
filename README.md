# 乡村振兴管理系统 · 后端 API

> Spring Boot 4.0.2 + JPA + MySQL + JWT · 124 个 Java 文件 · 19 张数据库表 · 80+ API 接口

## 快速开始

### 环境要求

- Java 17+
- Maven 3.8+
- MySQL 5.7+（推荐 8.0）

### 启动

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS rural_revitalization DEFAULT CHARSET utf8mb4;"

# 2. 修改 application.yml 中数据库账号密码
# 3. 启动
mvn spring-boot:run
```

JPA 自动建表。启动后自动创建 3 个测试账号：

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `admin` | `admin123` | 管理员 | 全局管理 |
| `bank01` | `123456` | 金融服务商 | 农村信用社牟平支行 |
| `insure01` | `123456` | 保险提供商 | 中国人保财险烟台分公司 |

Swagger 文档: http://localhost:8080/swagger-ui.html

### 测试文件

项目根目录下 4 个 `.txt` 文件是 JetBrains HTTP Client 格式的测试脚本：

| 文件 | 覆盖模块 |
|------|----------|
| `api-test-tourism.txt` | 旅游景点、评论、收藏、管理员审核 |
| `api-test-cart-order.txt` | 购物车、订单、发货 |
| `api-test-booking-route.txt` | 票务、预约、路线规划 |
| `api-test-finance.txt` | 贷款、保险、财务看板、信用报告 |
| `api-test-product-tags.txt` | 标签、补货、调价、购买人次 |
| `api-test-address.txt` | 地址簿CRUD、默认地址、下单集成、快照验证 |
| `api-test-upload.txt` | 图片上传、静态访问、错误场景、产品集成 |

下载后重命名 `.txt` → `.http`，在 IntelliJ IDEA 中直接运行。

---

## 系统架构

### 整体分层

```
┌──────────────────────────────────────────────────────────────────┐
│                        前端 (Vue / React / 小程序)                │
│    Authorization: Bearer <token>  ←──  登录后所有请求带此头       │
└───────────────────────────────┬──────────────────────────────────┘
                                │ HTTP JSON
┌───────────────────────────────▼──────────────────────────────────┐
│  Controller 层                                                    │
│  接收请求 → 参数校验(@Valid) → 调用 Service → 包装 Result 返回     │
├──────────────────────────────────────────────────────────────────┤
│  Service 层                                                       │
│  业务逻辑 → 权限判断 → 数据组装 → 调用 Repository                 │
│                                                                   │
│  跨模块调用:                                                       │
│    Financial ──读取──→ Order(收入) + Product(商品) + Policy(保险)   │
│    Loan ──调用──→ Financial(信用评分)                               │
│    Booking ──读取──→ TourismSpot(景点) + TicketType(票型)          │
├──────────────────────────────────────────────────────────────────┤
│  Repository 层 (JPA)                                              │
│  数据库操作，Spring Data 自动生成 SQL                              │
├──────────────────────────────────────────────────────────────────┤
│  MySQL · 18 张表 · JPA 自动建表(ddl-auto: update)                 │
└──────────────────────────────────────────────────────────────────┘
```

### 数据流转

```
前端 JSON → Request DTO(@Valid校验) → Service → Entity ↔ JPA ↔ MySQL
                                                  ↓
                                   Response DTO → Result<T> → 前端 JSON
```

为什么用 DTO 而不直接返回 Entity?
1. Entity 有密码字段，直接返回会泄露
2. Response 可附加信息（如分类名、卖家名、信用评分等级）
3. Request 可加校验注解，前后端字段不一定对应

---

## 统一响应格式

所有接口返回:

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 / 业务校验失败（message 有具体中文原因） |
| 401 | 未登录 / Token 过期 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

前端统一处理: `if (res.data.code !== 200) alert(res.data.message)`

### 分页响应

分页接口的 `data`:

```json
{
  "records": [ ... ],
  "total": 56,
  "page": 1,
  "size": 10,
  "totalPages": 6
}
```

### 认证方式

登录后获得 JWT Token，后续请求 Header 携带:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

Token 有效期 24 小时。Token 内含 `userId`, `username`, `role`。

---

## 角色体系

| 角色 | 英文标识 | 注册方式 | 说明 |
|------|----------|----------|------|
| 游客 | `TOURIST` | 默认 | 浏览/购物/预约 |
| 农户 | `FARMER` | `role: "FARMER"` | 发布农产品 + 金融服务 |
| 商家 | `MERCHANT` | `role: "MERCHANT"` | 同农户 |
| 金融服务商 | `FINANCIAL_PROVIDER` | 需填 orgName + licenseNo | 发布贷款产品 + 审核 |
| 保险提供商 | `INSURANCE_PROVIDER` | 需填 orgName + licenseNo | 发布保险产品 + 审核理赔 |
| 管理员 | `ADMIN` | 仅初始化创建 | 全局管理 |

金融/保险机构注册:

```json
POST /api/v1/auth/register
{
  "username": "bank02", "password": "123456", "confirmPassword": "123456",
  "role": "FINANCIAL_PROVIDER",
  "orgName": "某某农商银行XX支行",
  "licenseNo": "FIN-2026-002"
}
```

---

## 全部模块 · API 接口 · 返回数据结构

### 模块一：用户认证

> 表: `user`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/v1/auth/register` | 公开 | 注册 |
| POST | `/api/v1/auth/login` | 公开 | 登录，返回 Token |
| GET | `/api/v1/users/me` | 登录 | 获取个人信息 |
| PUT | `/api/v1/users/me` | 登录 | 修改昵称/手机/邮箱/头像 |
| PUT | `/api/v1/users/me/password` | 登录 | 修改密码 |

登录返回:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userInfo": {
    "id": 2, "username": "farmer01", "nickname": "张三果园",
    "role": "FARMER", "avatar": null
  }
}
```

### 模块二：管理员用户管理

> 需 ADMIN 角色

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/admin/users?keyword=&role=&page=1&size=10` | 用户列表 |
| GET | `/api/v1/admin/users/{id}` | 详情 |
| PUT | `/api/v1/admin/users/{id}` | 改角色/状态 |
| DELETE | `/api/v1/admin/users/{id}` | 删除 |
| PUT | `/api/v1/admin/users/{id}/reset-pwd` | 重置密码→123456 |

### 模块三：农产品

> 表: `product`, `product_category`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/products?keyword=&categoryId=&minPrice=&maxPrice=&tag=有机&sort=newest&page=1&size=10` | 公开 | 产品列表(多条件) |
| GET | `/api/v1/products/{id}` | 公开 | 详情 |
| GET | `/api/v1/products/categories` | 公开 | 分类树 |
| GET | `/api/v1/products/tags` | 公开 | 所有标签(去重排序) |
| POST | `/api/v1/products` | 登录 | 发布 |
| PUT | `/api/v1/products/{id}` | 发布者 | 全量编辑 |
| PATCH | `/api/v1/products/{id}` | 发布者 | 部分修改(补货/调价/改标签) |
| PUT | `/api/v1/products/{id}/status` | 发布者 | 上/下架 |
| DELETE | `/api/v1/products/{id}` | 发布者/管理员 | 删除 |
| GET | `/api/v1/products/mine` | 登录 | 我的产品 |
| POST | `/api/v1/admin/categories` | 管理员 | 添加分类 |

sort: `newest`, `price_asc`, `price_desc`, `sales_desc`, `buyers_desc`

PATCH 部分修改: 只传要改的字段，其他不变
- 补货: `{ "stock": 500 }` — 售罄自动恢复上架
- 调价: `{ "price": 3.99 }`
- 标签: `{ "tags": "有机,绿色食品" }` / `{ "tags": "" }` 清除

产品返回:

```json
{
  "id": 1, "name": "红富士苹果", "description": "山东烟台正宗红富士",
  "price": 5.50, "stock": 500, "salesCount": 120, "buyerCount": 45,
  "unit": "斤", "origin": "山东烟台",
  "tags": "有机,绿色食品,助农",
  "imageUrl": "https://...",
  "categoryId": 2, "categoryName": "苹果",
  "sellerId": 2, "sellerName": "张三果园",
  "status": "ON_SALE",
  "createdAt": "2026-02-25 10:30:00"
}
```

### 模块四：购物车

> 表: `cart_item`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/cart` | 加入购物车 `{ "productId": 1, "quantity": 3 }` |
| GET | `/api/v1/cart` | 查看购物车 |
| PUT | `/api/v1/cart/{id}` | 改数量/选中 |
| PUT | `/api/v1/cart/select-all` | 全选/全不选 |
| DELETE | `/api/v1/cart/{id}` | 删除某项 |
| DELETE | `/api/v1/cart` | 清空 |
| GET | `/api/v1/cart/count` | 商品数 |

购物车返回:

```json
{
  "items": [{
    "id": 1, "productId": 1, "productName": "红富士苹果",
    "productImage": "...", "unitPrice": 5.50, "quantity": 3,
    "unit": "斤", "subtotal": 16.50, "selected": true,
    "stock": 500, "productStatus": "ON_SALE"
  }],
  "totalCount": 1, "selectedCount": 1, "selectedAmount": 16.50
}
```

### 模块五：地址簿

> 表: `address`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/addresses` | 当前用户的地址列表(默认排前) |
| POST | `/api/v1/addresses` | 新增收货地址 |
| PUT | `/api/v1/addresses/{id}` | 修改收货地址 |
| DELETE | `/api/v1/addresses/{id}` | 删除收货地址 |
| PUT | `/api/v1/addresses/{id}/default` | 设为默认地址 |

新增请求:
```json
{
  "receiverName": "李小明",
  "receiverPhone": "13900139001",
  "province": "山东省",
  "city": "烟台市",
  "district": "芝罘区",
  "detail": "胜利路168号幸福花园3号楼201",
  "isDefault": false
}
```

地址返回:
```json
{
  "id": 1, "receiverName": "李小明", "receiverPhone": "13900139001",
  "province": "山东省", "city": "烟台市", "district": "芝罘区",
  "detail": "胜利路168号幸福花园3号楼201",
  "fullAddress": "山东省烟台市芝罘区胜利路168号幸福花园3号楼201",
  "isDefault": true
}
```

业务规则:
- 每用户最多 20 个地址，第一个自动设为默认
- 设新默认时旧默认自动取消（始终只有一个默认）
- `fullAddress` 由后端自动拼接 province+city+district+detail
- 地址只能本人操作（userId 绑定），买家看不到商家的地址
- 商家也可以管理地址（用于发货地址）

### 模块六：订单

> 表: `orders`, `order_item`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/v1/orders` | 从已选购物车商品创建订单 |
| GET | `/api/v1/orders?status=&page=1&size=10` | 我的订单 |
| GET | `/api/v1/orders/{id}` | 详情 |
| PUT | `/api/v1/orders/{id}/pay` | 支付 |
| PUT | `/api/v1/orders/{id}/ship` | 发货(卖家) |
| PUT | `/api/v1/orders/{id}/confirm` | 确认收货 |
| PUT | `/api/v1/orders/{id}/cancel` | 取消 |

下单支持两种方式:
```json
// 方式1（推荐）: 地址簿ID
{ "addressId": 1, "remark": "请周末配送" }

// 方式2（兼容旧版）: 手动输入
{ "shippingAddress": "山东省烟台市...", "receiverName": "李小明",
  "receiverPhone": "13900139001", "remark": "..." }
```
addressId 优先; 地址信息快照到订单（后续修改地址簿不影响已有订单）

状态: `PENDING_PAYMENT → PAID → SHIPPED → COMPLETED` (或 `CANCELLED`)

### 模块七：旅游推广

> 表: `tourism_spot`, `tourism_review`, `tourism_favorite`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/tourism/spots?keyword=&type=&status=&page=1&size=10` | 公开 | 景点列表 |
| GET | `/api/v1/tourism/spots/{id}` | 公开 | 详情(浏览量+1) |
| POST | `/api/v1/tourism/spots` | 商家/管理员 | 发布(商家需审核) |
| PUT | `/api/v1/tourism/spots/{id}` | 发布者 | 编辑 |
| DELETE | `/api/v1/tourism/spots/{id}` | 发布者/管理员 | 删除 |
| GET | `/api/v1/tourism/spots/mine` | 登录 | 我的景点 |
| GET | `/api/v1/tourism/spots/{id}/reviews?page=1&size=10` | 公开 | 评论列表 |
| POST | `/api/v1/tourism/spots/{id}/reviews` | 登录 | 发评论 |
| DELETE | `/api/v1/tourism/reviews/{id}` | 自己/管理员 | 删评论 |
| POST | `/api/v1/tourism/spots/{id}/favorite` | 登录 | 收藏/取消(切换) |
| GET | `/api/v1/tourism/favorites?page=1&size=10` | 登录 | 我的收藏 |
| GET | `/api/v1/admin/tourism/spots?status=PENDING` | 管理员 | 待审核列表 |
| PUT | `/api/v1/admin/tourism/spots/{id}/audit` | 管理员 | 审核 |

景点类型: `PICKING_GARDEN`, `FARMSTAY`, `ECO_PARK`, `FOLK_EXPERIENCE`, `SCENIC_SPOT`

### 模块八：旅游预约

> 表: `tourism_ticket`, `tourism_booking`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/tourism/spots/{spotId}/tickets?date=2026-03-15` | 公开 | 票型(带date返回余量) |
| POST | `/api/v1/tourism/spots/{spotId}/tickets` | 景点主/管理员 | 创建票型 |
| PUT | `/api/v1/tourism/spots/{spotId}/tickets/{id}` | 景点主/管理员 | 编辑 |
| DELETE | `/api/v1/tourism/spots/{spotId}/tickets/{id}` | 景点主/管理员 | 删除 |
| POST | `/api/v1/tourism/bookings` | 登录 | 创建预约 |
| GET | `/api/v1/tourism/bookings?status=&page=1&size=10` | 登录 | 我的预约 |
| GET | `/api/v1/tourism/bookings/{id}` | 登录 | 详情 |
| PUT | `/api/v1/tourism/bookings/{id}/pay` | 登录 | 支付 |
| PUT | `/api/v1/tourism/bookings/{id}/cancel` | 登录 | 取消 |
| PUT | `/api/v1/tourism/bookings/verify` | 商家/管理员 | 核验预约码 |

票型分类: `TICKET`(门票, 指定visitDate) / `ROOM`(住宿, 指定checkIn+checkOut)

预约状态: `PENDING_PAYMENT → CONFIRMED → USED → COMPLETED` (或 `CANCELLED`)

预约返回:

```json
{
  "id": 1, "bookingCode": "A3K7M2",
  "spotName": "张三采摘园", "ticketName": "成人票", "ticketCategory": "TICKET",
  "unitPrice": 68.00, "quantity": 2, "nights": 0, "totalAmount": 136.00,
  "visitDate": "2026-03-15", "checkIn": null, "checkOut": null,
  "contactName": "小王", "contactPhone": "13900139000",
  "status": "PENDING_PAYMENT",
  "createdAt": "...", "paidAt": null, "usedAt": null, "cancelledAt": null
}
```

### 模块九：路线规划

> 表: `tourism_route`, `tourism_route_item`

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/tourism/routes/public?page=1&size=10` | 公开 | 推荐路线(按引用数排序) |
| GET | `/api/v1/tourism/routes/{id}` | 公开/创建者 | 详情(私有仅创建者) |
| POST | `/api/v1/tourism/routes` | 登录 | 创建 |
| PUT | `/api/v1/tourism/routes/{id}` | 创建者 | 编辑(全量替换行程) |
| DELETE | `/api/v1/tourism/routes/{id}` | 创建者 | 删除 |
| GET | `/api/v1/tourism/routes/mine?page=1&size=10` | 登录 | 我的路线 |
| POST | `/api/v1/tourism/routes/{id}/copy` | 登录 | 复制公开路线 |

路线返回:

```json
{
  "id": 1, "title": "烟台周末两日游",
  "creatorName": "小王", "totalDays": 2, "startDate": "2026-03-20",
  "isPublic": true, "coverImage": "...", "copyCount": 3,
  "days": [
    {
      "dayNumber": 1, "date": "2026-03-20",
      "spots": [
        { "itemId": 1, "spotId": 1, "spotName": "张三采摘园",
          "coverImage": "...", "type": "PICKING_GARDEN", "address": "...",
          "sortOrder": 1, "visitTime": "09:00", "durationMinutes": 180,
          "notes": "上午摘草莓" }
      ]
    },
    { "dayNumber": 2, "date": "2026-03-21", "spots": [...] }
  ]
}
```

### 模块十：贷款服务

> 表: `loan_product`, `loan_application` · 金融服务商发布+审核, 农户申请

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/finance/loan-products?page=1&size=10` | 登录 | 浏览产品 |
| POST | `/api/v1/finance/loan-products` | 金融商/管理员 | 发布 |
| PUT | `/api/v1/finance/loan-products/{id}` | 金融商/管理员 | 全量编辑 |
| PATCH | `/api/v1/finance/loan-products/{id}` | 金融商/管理员 | 部分修改(只传要改的字段) |
| DELETE | `/api/v1/finance/loan-products/{id}` | 金融商/管理员 | 下线 |
| GET | `/api/v1/finance/loan-products/mine` | 金融商 | 我的产品 |
| POST | `/api/v1/finance/loan-applications` | 农户/商家 | 申请贷款 |
| GET | `/api/v1/finance/loan-applications/mine` | 农户/商家 | 我的申请 |
| GET | `/api/v1/finance/loan-applications/review?status=SUBMITTED` | 金融商 | 待审核 |
| PUT | `/api/v1/finance/loan-applications/{id}/review` | 金融商/管理员 | 审核 |

贷款类型: `CROP_LOAN`, `EQUIPMENT_LOAN`, `WORKING_CAPITAL`, `LAND_LOAN`, `AGRI_CHAIN`

申请状态: `SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED → DISBURSED → REPAID`

申请时自动附信用评分快照, 分数低于产品门槛直接拒绝。

PATCH 部分修改: `{ "minCreditScore": 35 }` — 只改门槛，其他字段不变

### 模块十一：保险服务

> 表: `insurance_product`, `insurance_policy`, `insurance_claim` · 保险商发布+审核, 农户投保

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/finance/insurance-products?page=1&size=10` | 登录 | 浏览产品 |
| POST | `/api/v1/finance/insurance-products` | 保险商/管理员 | 发布 |
| PUT/PATCH | `/api/v1/finance/insurance-products/{id}` | 保险商/管理员 | 编辑(支持部分修改) |
| GET | `/api/v1/finance/insurance-products/mine` | 保险商 | 我的产品 |
| POST | `/api/v1/finance/policies` | 农户/商家 | 投保 |
| PUT | `/api/v1/finance/policies/{id}/pay` | 持有人 | 支付保费 |
| GET | `/api/v1/finance/policies/mine` | 农户/商家 | 我的保单 |
| POST | `/api/v1/finance/claims` | 农户/商家 | 理赔报案 |
| GET | `/api/v1/finance/claims/mine` | 农户/商家 | 我的理赔 |
| GET | `/api/v1/finance/claims/review?status=SUBMITTED` | 保险商 | 待审核理赔 |
| PUT | `/api/v1/finance/claims/{id}/review` | 保险商/管理员 | 审核理赔 |

保险类型: `CROP`, `LIVESTOCK`, `FORESTRY`, `FACILITY`, `INCOME`

保单状态: `PENDING_PAYMENT → ACTIVE → EXPIRED/CLAIMED/CANCELLED`

理赔状态: `SUBMITTED → UNDER_REVIEW → APPROVED/REJECTED → PAID_OUT`

自动计算: `总保费 = 单价 × 数量`, `总保额 = 保额 × 数量`

校验: 出险日期须在保障期内, 索赔不超过保额, 同保单不可重复理赔

### 模块十二：财务分析 + 信用评分

> 无新表, 跨模块聚合 orders + product + insurance_policy 数据

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | `/api/v1/finance/dashboard` | 农户/商家 | 我的财务看板 |
| GET | `/api/v1/finance/dashboard/{farmerId}` | 管理员 | 查看指定农户(金融商不可) |
| GET | `/api/v1/finance/credit-report` | 农户/商家 | 我的信用报告 |
| GET | `/api/v1/finance/credit-report/{farmerId}` | 管理员/有关联金融商 | 查看指定农户 |

财务看板返回:

```json
{
  "totalRevenue": 126800.00,
  "monthRevenue": 12500.00,
  "lastMonthRevenue": 10200.00,
  "revenueGrowthRate": 22.55,
  "totalCompletedOrders": 234,
  "activeOrders": 8,
  "activeProducts": 12,
  "monthlyTrend": [
    { "month": "2025-03", "revenue": 8500.00, "orderCount": 15 },
    { "month": "2025-04", "revenue": 9200.00, "orderCount": 18 }
  ],
  "creditReport": {
    "totalScore": 82, "grade": "EXCELLENT",
    "historyScore": 25, "tradeScore": 35, "riskScore": 22,
    "registeredDays": 456, "activeProductCount": 12,
    "completedOrderCount": 234, "completionRate": 98.3,
    "totalTransactionAmount": 126800.00,
    "recentMonthRevenue": 12500.00, "activePolicyCount": 2,
    "generatedAt": "2026-02-25 15:00:00"
  },
  "activePolicies": 2,
  "totalCoverage": 80000.00,
  "suggestions": [
    "📈 本月收入同比增长22.55%，可考虑申请设备购置贷支持扩产",
    "⭐ 信用评分82分（优秀），可申请更高额度的贷款产品",
    "🌱 春耕备耕时节，可关注「春耕贷」等低利率贷款产品"
  ]
}
```

**信用评分模型（满分100）:**

| 维度 | 满分 | 评分规则 |
|------|------|----------|
| 经营历史 | 30 | 注册时长(≥365天→10) + 在售商品数(≥10款→10) + 近期活跃(30天内有收入→10) |
| 交易信用 | 40 | 完成率(≥95%→20) + 成交额(≥10万→10) + 零取消(→10) |
| 风险评估 | 30 | 收入趋势(本月≥上月→10) + 有保险(→10) + 无逾期(→10) |

等级: `EXCELLENT`(≥80) / `GOOD`(60-79) / `FAIR`(40-59) / `POOR`(<40)

### 模块十三：图片上传

> 本地文件存储，按日期分目录

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/v1/upload/image` | 登录 | 上传图片 |
| GET | `/uploads/2026/03/01/xxx.jpg` | 公开 | 访问已上传图片 |

上传请求: `Content-Type: multipart/form-data`, 字段名 `file`

返回:
```json
{ "url": "/uploads/2026/03/01/a1b2c3d4.jpg", "filename": "a1b2c3d4.jpg" }
```

限制: JPG/PNG/GIF/WebP, 最大 5MB。存储路径: `uploads/年/月/日/UUID.ext`

前端用法: 上传后将 `data.url` 填入产品的 `imageUrl`、景点的封面等字段

---

## 权限矩阵

| 操作 | 未登录 | TOURIST | FARMER | FIN_PROVIDER | INS_PROVIDER | ADMIN |
|---|---|---|---|---|---|---|
| 浏览商品/景点 | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 发布农产品/景点 | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ |
| 购物车/下单 | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 预约门票 | ❌ | ✅ | ✅ | ✅ | ✅ | ✅ |
| 发布贷款产品 | ❌ | ❌ | ❌ | ✅ | ❌⛔ | ✅ |
| 申请贷款 | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 发布保险产品 | ❌ | ❌ | ❌ | ❌⛔ | ✅ | ✅ |
| 投保/理赔 | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ |
| 财务看板 | ❌ | ❌ | ✅自己 | ❌🔒 | ❌ | ✅任何人 |
| 用户管理 | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ |

> ⛔ 金融商与保险商严格隔离   🔒 金融商不能直接查看农户财务

---

## 数据库 ER 概览

```
user (6种角色)
 ├──→ product ←── product_category
 ├──→ cart_item
 ├──→ address (收货/发货地址簿)
 ├──→ orders ──→ order_item ──→ product
 │     └──→ address (快照引用)
 ├──→ tourism_spot
 │     ├──→ tourism_review
 │     ├──→ tourism_favorite
 │     ├──→ tourism_ticket ──→ tourism_booking
 │     └──→ tourism_route_item ──→ tourism_route
 ├──→ loan_product ──→ loan_application
 └──→ insurance_product ──→ insurance_policy ──→ insurance_claim
```

共 19 张表, JPA `ddl-auto: update` 自动建表

---

## 安全机制

| 机制 | 说明 |
|------|------|
| BCrypt | 密码加密存储 |
| JWT 24h | Token 含 userId + role |
| 路径权限 | admin路径仅ADMIN, finance路径需登录 |
| 资源归属 | 修改/删除时校验是否本人发布 |
| 金融隔离 | 金融商与保险商不可跨界操作 |
| 隐私保护 | 农户财务数据仅自己和管理员可见 |
| 快照冻结 | 下单冻结价格+地址, 贷款申请冻结信用分 |
| 容量保护 | 预约实时检查余量防超卖 |
| 防重复 | 同名产品/重复申请/重复理赔拦截 |
