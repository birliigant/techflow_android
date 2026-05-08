# SIPC TechFlow Android

一个基于 Apache Answer 后端接口构建的原生 Android 客户端，面向 `SIPC TechFlow` 学术问答社区的移动端使用场景。

本项目完全由 Codex 基于现有后端接口文档、Swagger 产物和网页端交互参考实现。开发过程中没有直接复用网页端源码作为 Android 客户端，而是以接口契约为依据，重新设计并落地了原生 Android 的架构、数据层、Compose 界面与移动端交互。

本项目当前以 [docs/接口文档-入口.md](docs/接口文档-入口.md)、[docs/接口文档.md](docs/接口文档.md)、[docs/swagger.json](docs/swagger.json) 与 [docs/swagger.yaml](docs/swagger.yaml) 作为主要后端接口依据，围绕「提问、浏览、详情、回答、用户主页、标签、个人资料」等核心场景，使用现代 Android 技术栈完成了一套可运行、可扩展、可持续演进的 Compose 客户端。

如果需要一份更偏“版本清单 / 技术归档”风格的说明，可以直接看 [docs/项目技术栈.md](docs/项目技术栈.md)。

## 1. 项目定位

这个项目不是一个简单的“Web 包壳”，而是一个基于 Android 原生能力实现的移动端应用：

- 使用 `Jetpack Compose` 构建界面
- 使用 `Retrofit + OkHttp` 访问后端接口
- 使用 `Room` 做本地问题缓存
- 使用 `MMKV` 保存 token、用户会话与轻量 UI 偏好
- 使用 `MVVM + Repository` 保持页面、状态、数据访问职责清晰
- 使用 `单 Activity + 多 Compose Screen` 组织整体导航结构

它的目标是：

- 在移动端提供比网页更顺手的阅读和交互体验
- 保持和现有 Apache Answer 后端兼容
- 为后续扩展通知、搜索、互动、编辑、离线缓存、更多社区能力预留良好结构

## 2. 当前技术栈

### 2.1 UI 层

- `Jetpack Compose`
- `Material 3`
- `Compose Navigation`
- `Coil`

说明：

- 虽然最初技术倾向里提到过 `Navigation3 / ARouter`，但当前项目已经稳定落地的是 `Navigation Compose`
- 这样做的原因是它与当前的 Compose UI 架构耦合更低、接入更直接、维护成本更小
- 如果未来需要切换到别的导航方案，当前页面结构和路由边界也足够清晰，迁移成本可控

### 2.2 数据层

- `Retrofit 2.11.0`
- `OkHttp 4.12.0`
- `Gson`
- `Kotlin Coroutines`
- `Room 2.6.1`
- `MMKV 1.3.5`
- `Bootstrap Icons 1.13.1`

### 2.3 Android / 构建层

- `AGP 8.13.0`
- `Kotlin 2.0.21`
- `KSP`
- `JDK 17`
- `minSdk 24`
- `targetSdk 36`

### 2.4 关键依赖版本

依赖统一收敛在 [gradle/libs.versions.toml](gradle/libs.versions.toml) 中管理，便于升级与维护。

## 3. 架构设计

项目采用典型的 `MVVM + Repository + 单 Activity` 架构。

### 3.1 整体分层

```mermaid
flowchart TD
    UI["Compose Screens / ViewModel"] --> Repo["Repository"]
    Repo --> Net["Retrofit + OkHttp API"]
    Repo --> Local["Room / MMKV"]
    Net --> Backend["Apache Answer Backend"]
```

### 3.2 分层职责

#### UI 层

位于 `app/src/main/java/com/birliigant/techflow/ui/`

负责：

- 页面渲染
- 用户交互
- 页面状态展示
- 路由跳转

当前主要页面包括：

- `home` 首页问题流
- `detail` 问题详情页
- `ask` 提问页
- `auth/login` 独立登录页
- `auth/register` 独立注册页
- `me` 我的页
- `settings` 账号设置页
- `profile` 用户主页
- `explore/tags` 标签页
- `explore/users` 用户页

#### ViewModel 层

ViewModel 与页面一一对应，负责：

- 发起异步请求
- 聚合多个仓库数据
- 管理页面状态
- 屏蔽 UI 对具体接口细节的感知

例如：

- `HomeViewModel`
- `QuestionDetailViewModel`
- `MeViewModel`
- `ProfileViewModel`
- `SettingsViewModel`

#### Repository 层

位于 [Repositories.kt](app/src/main/java/com/birliigant/techflow/data/repository/Repositories.kt)

负责：

- 对接口进行领域级封装
- 管理网络和本地缓存之间的协调
- 输出更适合 UI 消费的数据模型

当前仓库包括：

- `SiteRepository`
- `QuestionRepository`
- `UserRepository`
- `TagRepository`
- `SessionRepository`
- `ConfigRepository`

#### 数据源层

包括两部分：

- 远端：`Retrofit + OkHttp`
- 本地：`Room + MMKV`

职责划分：

- `OkHttp`：统一注入与 Web 端一致的 `Authorization: <token>` 请求头
- `Permission API`：在点赞前按 `question.vote_up` / `answer.vote_up` / `comment.vote_up` 做权限预检，并兼容 Apache Answer 按 action 分组返回的权限对象
- `Room`：保存问题列表缓存，用于网络失败时兜底显示
- `MMKV`：保存 access token、当前用户信息与轻量 UI 偏好状态
- `Runtime Permission`：首次启动会按系统版本申请必要运行时权限，后续在图片上传等具体功能使用前再次动态检查并按需申请
- `INTERNET`：网络访问权限属于普通安装时权限，Android 不提供运行时动态申请弹窗；只需要在 `AndroidManifest.xml` 中声明

### 3.3 为什么这样设计

这样设计的优点是：

- UI 和数据访问解耦
- 页面逻辑更容易测试和替换
- 后端字段变化时，优先只需要修改 network / repository 层
- 随着功能变多，不容易演变成“页面直接请求接口”的混乱结构

## 4. 项目目录说明

```text
app/src/main/java/com/birliigant/techflow
├── MainActivity.kt                  # 单 Activity 入口
├── TechFlowApplication.kt           # Application，初始化 MMKV / AppContainer
├── app/
│   └── AppContainer.kt              # 轻量依赖注入容器
├── core/
│   └── model/Models.kt              # 领域模型定义
├── data/
│   ├── local/QuestionCache.kt       # Room 实体、DAO、Database
│   ├── network/NetworkModels.kt     # DTO / 网络模型 / 映射
│   ├── network/TechFlowApi.kt       # Retrofit API 定义
│   └── repository/Repositories.kt   # Repository 实现
└── ui/
    ├── auth/                        # 独立登录、注册页面
    ├── ask/                         # 提问页
    ├── common/                      # 公共 UI 组件、Markdown 渲染
    ├── detail/                      # 问题详情
    ├── explore/                     # 标签页、用户页
    ├── home/                        # 首页
    ├── me/                          # 我的页
    ├── navigation/                  # 应用导航入口
    ├── profile/                     # 用户主页
    ├── settings/                    # 账号设置
    └── theme/                       # 主题、配色
```

## 5. 当前已实现功能

### 5.1 首页问题流

- 展示问题列表
- 支持多种排序：
  - 最新
  - 活跃
  - 热门
  - 评分
  - 未回答
  - 推荐
- 如果问题已经有最佳回答，首页回答统计会以绿色高亮并显示采纳图标，尽量对齐 Web 端语义
- 首页问题卡片做了标题、摘要、作者、统计、标签的分层排布，尽量避免信息拥挤
- 支持首页下拉刷新，用户可以手动拉取最新问题流
- 支持从首页点击作者进入用户主页
- 支持从首页进入标签页、用户页、我的页

### 5.2 问题详情页

- 展示问题标题、作者、统计信息
- 展示问题正文
- 支持 Markdown / HTML 内容渲染
- 展示回答列表
- 展示评论列表
- 支持帖子点赞、收藏、评论、分享、删除、举报
- 支持回答点赞、楼中楼评论、分享、删除、举报
- 详情页操作栏支持横向滚动，小屏下新增删除后仍能访问举报等后续操作
- 删除整帖成功后自动回到首页，并重建首页数据流，避免停留在已删除详情页
- 点赞前会拦截给自己内容投票，并解析权限预检与后端 403 错误体；当服务端要求声望门槛时，会展示中文提示而不是继续发送无效投票请求
- 点赞、删除等操作会直接刷新当前卡片的本地状态，成功、失败和权限不足等操作反馈统一通过 Toast 展示
- 回答作者、评论作者过长时会自动省略，避免压缩日期与操作区布局
- 支持点击回答者、评论者、被回复用户进入对应主页

### 5.3 登录与会话

- 支持邮箱密码登录
- 登录与注册是独立路由页面，不再嵌入“我的”页面
- 密码输入默认隐藏，并提供眼睛图标用于临时显示 / 隐藏
- 登录成功后通过带缓冲的一次性事件 Toast 提示，短暂展示提示后清理认证路由栈并自动进入“我的”页面
- 注册成功后通过一次性事件 Toast 提示并自动回到登录页
- 登录后自动刷新当前用户资料
- 进入“我的”或“账号设置”页面时自动刷新用户态
- 支持退出登录

### 5.4 我的页面

- 未登录时只展示简洁空状态和登录 / 注册入口
- 登录后展示头像、用户名、邮箱、Rank、问题/回答数量
- 提供用户主页、收藏夹、账号设置等入口

### 5.5 用户主页

- 支持查看任意用户主页
- 支持概览 / 回答 / 问题 / 收藏 / 声望 / 评论 / 得票 / 徽章视图
- 展示基础资料、专业、简介、最近内容
- 作为社区内“人物维度”的重要信息入口

### 5.6 账号设置

- 支持编辑并保存：
  - 显示名称
  - 用户名
  - 专业
  - 地区
  - 网站
  - 个人简介
- 当前头像为只读展示

### 5.7 标签与用户发现

- 标签页支持按分区展示标签
- 用户页支持展示社区用户列表
- 所有这些入口都可以从首页和顶部菜单触达

### 5.8 提问

- 支持创建问题
- 支持填写标题、正文、标签、分区等信息
- 发帖请求对齐 Web 端实际行为提交 `title/content/partition/tags`；细分类标签可选，不填时自动使用当前分区下的“其他”标签
- 标签选择对齐 Web 端：按分区展示常用标签，支持点选/取消，并通过加号打开“创建新标签”对话框把自定义标签加入本次提问
- 发帖提交会立即锁定按钮，创建问题接口使用较短超时避免长时间卡住；如果后端已创建但客户端读超时，会先 Toast 提示进入待确认状态，再短轮询最新问题和搜索结果，确认后自动进入问题详情
- 支持更接近 Web 端的 Markdown 编辑器式输入区域
- 支持标题、加粗、斜体、代码块、超链接、引用、图片上传、表格、有序列表、无序列表、缩进、减少缩进、水平线等工具栏操作
- 支持跳转到 `https://commonmark.org/help/` 查看 CommonMark 帮助
- 支持基于正文输入的实时 Markdown 预览
- 上传图片前会动态检查媒体读取权限，未授权时先唤起系统权限申请，通过后再打开图片选择器

### 5.9 缓存与兜底

- 问题列表会写入 Room
- 当请求问题列表失败时，优先回退到最近一次缓存
- 问题详情在接口异常时，也会尽量从本地缓存构造兜底内容

### 5.10 运行时权限

- 首次打开 App 时会先展示权限说明，明确网络访问权限属于安装时自动授予的普通权限；随后根据 Android 版本动态申请可运行时授权的系统权限，例如 Android 13+ 的通知权限、图片访问权限
- 后续功能使用前会再次检查权限，例如提问页图片上传会在打开选择器前检查图片访问权限
- Android 系统权限弹窗文案由系统语言控制，App 内会先展示中文说明弹窗，再唤起系统授权弹窗
- 权限申请状态会通过带版本号的 `MMKV` 记录，避免每次冷启动都重复打扰用户，也便于新增权限后重新触发检查
- 网络权限 `INTERNET` 不是危险权限，不支持运行时动态申请；客户端会在首次权限说明中展示它的授权状态，但不会出现系统授权弹窗

## 6. 项目特点

### 6.1 不是照搬网页，而是做了移动端适配

项目并没有简单把网页结构硬搬进 App，而是做了几层移动端优化：

- 重要信息优先级重排
- 详情内容支持原生滚动和阅读
- 顶部操作区和用户菜单更适合触屏场景
- 底部导航使用自定义紧凑栏，降低默认 Material3 导航栏占用高度
- 首页工具菜单、用户菜单和我的页更多菜单都会锚定到触发按钮，并保留统一的下方间距
- 列表和卡片做了更轻量的视觉压缩

### 6.2 对后端字段不稳定有兼容处理

由于现网接口返回并不总是完全严格一致，项目做了较多兼容映射，例如：

- `avatar` 可能是字符串，也可能是对象
- 时间字段可能是 `created_at` 或 `create_time`
- 正文字段可能落在 `content` / `parsed_text` / `html`
- `accepted` 可能是布尔、字符串或数值
- 首页列表里“是否有最佳回答”既兼容 `accepted`，也兼容 `accepted_answer_id`
- 回答列表里的“是否已采纳”以问题详情返回的 `accepted_answer_id` 为准，避免直接误用回答流里的不稳定 `accepted`
- 用户主页信息既兼容 Swagger 中的 `data.info` 包装，也兼容线上直接平铺在 `data` 下的结构
- 徽章列表既兼容 Swagger 中的 `data=array[...]`，也兼容线上实际返回的 `data={ count, list }`
- 徽章图标对齐 Web 端解析逻辑：`icon` 以 `http` 开头时按远程图片加载，否则按 Bootstrap Icons 名称映射本地矢量图标；默认徽章已覆盖 `person-badge-fill`、`pencil-fill`、`flag-fill`、`hand-thumbs-up-fill`、`emoji-smile-fill`、`share-fill`、`check-circle-fill`、`check-square-fill`、`chat-square-text-fill`、`question-circle-fill` 和 `blue_bridge_cup` 等标识，并按 `level=1/2/3` 或 `bronze/silver/gold` 展示铜、银、金颜色
- 发帖请求里的 `tags` 已按 Swagger 要求发送为 `schema.TagItem` 对象数组，而不是纯字符串数组
- 发帖接口的超时和 400 场景会短轮询“最新问题 + 搜索结果”做兜底确认，用于兼容后端已落库但客户端收到超时、重复提交或不稳定错误提示的情况

### 6.3 Swagger 与现网双基准

为了尽量避免“详细版摘要没展开完整 schema”带来的误判，项目当前遵循这样的接口校准顺序：

- 先看 [docs/接口文档.md](docs/接口文档.md) 快速定位接口
- 再看 [docs/swagger.json](docs/swagger.json) / [docs/swagger.yaml](docs/swagger.yaml) 获取完整请求体、响应体和模型引用
- 如果 Swagger 与现网真实返回不一致，再在映射层做兼容处理

这些兼容逻辑集中在 [NetworkModels.kt](app/src/main/java/com/birliigant/techflow/data/network/NetworkModels.kt) 和 [Repositories.kt](app/src/main/java/com/birliigant/techflow/data/repository/Repositories.kt) 中。

### 6.4 结构轻，但扩展空间大

项目没有引入过重的 DI 框架，而是使用 [AppContainer.kt](app/src/main/java/com/birliigant/techflow/app/AppContainer.kt) 做轻量依赖管理。

这样做的好处是：

- 当前项目体量下更简单直接
- 更适合快速迭代
- 未来如有需要，仍可平滑迁移到 `Hilt / Koin`

### 6.5 本地缓存策略简单但实用

当前缓存只覆盖问题列表和部分详情兜底，但对一个社区类 App 来说已经足以明显改善：

- 弱网下首页空白问题
- 接口偶发失败时的完全不可用问题

## 7. 运行方式

### 7.1 环境要求

- Android Studio 最新稳定版或较新版本
- JDK 17
- Android SDK 36

### 7.2 默认后端地址

项目默认后端地址为：

```text
https://answer.sipc115.com/
```

定义位置见 [Models.kt](app/src/main/java/com/birliigant/techflow/core/model/Models.kt) 中的 `AppDefaults.defaultBaseUrl`。

### 7.3 编译方式

在项目根目录执行：

```bash
./gradlew :app:assembleDebug
```

生成的 APK 默认位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 8. 当前实现与原始需求的关系

项目最初目标技术栈包括：

- UI：Compose
- 数据库：Room
- 网络：OkHttp / Retrofit
- 路由：Navigation3 / ARouter
- 本地存储：MMKV / SP
- 架构：MVVM，单 Activity + 多 Compose 组件

当前落地情况如下：

| 方向 | 当前实现 |
| --- | --- |
| UI | Compose |
| 数据库 | Room |
| 网络 | OkHttp + Retrofit |
| 本地存储 | MMKV |
| 架构 | MVVM + Repository + 单 Activity |
| 路由 | Navigation Compose |

说明：

- `Navigation3 / ARouter` 目前没有接入
- 当前导航层已经足够稳定，未来若有强需求可以替换，但不是现阶段的必要瓶颈

## 9. 已知边界与现状说明

虽然项目已经具备较完整的主流程，但仍有一些边界需要明确：

- 发帖编辑器已接入图片上传，用户头像上传仍未开放为完整的资料编辑能力
- 通知系统页面和消息中心还未完整落地
- 分区页、通知页、更多个人中心能力仍可进一步丰富

换句话说，当前项目已经是“可使用的 MVP+”，但还没有到“功能完全对齐网页端”的阶段。

## 10. 后续可扩展方向

这个项目的扩展空间很大，比较推荐的演进路线有：

### 10.1 社区互动能力补全

- 更完整的评论线程管理
- 点踩、撤销举报、更多 reaction 交互
- 收藏分组与书签管理
- 关注标签
- 关注用户

### 10.2 搜索体验增强

- 搜索历史
- 热门搜索
- 多维筛选
- 更完整的高级搜索语法支持
- 按标签 / 用户 / 标题 / 内容检索的进一步精细化

### 10.3 通知体系

- 收件箱
- 成就提醒
- 红点状态同步
- 已读 / 全部已读
- 推送通知接入

### 10.4 富文本编辑能力

- 代码块高亮
- 草稿自动保存
- 本地未提交内容恢复

### 10.5 更完整的离线能力

- 详情缓存
- 用户资料缓存
- 收藏本地镜像
- 增量同步策略

### 10.6 工程化升级

- 引入 `Hilt`
- 增加单元测试 / UI 测试
- 增加 CI
- 增加静态检查
- 拆分 feature module

## 11. 为什么这个项目值得继续做

如果把这个项目继续打磨下去，它会有几个很明显的价值：

- 能成为社区真正可用的 Android 客户端
- 能把原有网页端能力逐步沉淀成更顺手的移动端体验
- 能作为一个完整的 Compose + MVVM + Retrofit + Room 的中型示例项目
- 能为后续更多校园社区 / 问答社区产品提供模板

## 12. 参考文档

- 接口文档入口：[docs/接口文档-入口.md](docs/接口文档-入口.md)
- 接口文档：[docs/接口文档.md](docs/接口文档.md)
- Swagger 原始定义：[docs/swagger.json](docs/swagger.json) / [docs/swagger.yaml](docs/swagger.yaml)
- 原始品牌 Logo：[docs/img/logo.svg](docs/img/logo.svg)
- App 图标源文件：[docs/img/app-logo.svg](docs/img/app-logo.svg)，Android 启动图标已使用该图标语义替换旧的默认 Android 图标，并按 Adaptive Icon 安全区缩放居中，避免启动页裁切
- 首页 / UI 参考图：`docs/img/` 目录下相关图片资源

## 13. 总结

`SIPC TechFlow Android` 当前已经具备一个原生社区客户端的基本骨架：

- 有明确的分层
- 有清晰的导航
- 有核心内容流
- 有账户体系
- 有详情阅读
- 有用户主页
- 有设置与资料编辑
- 有本地缓存与接口兼容处理

它已经不是一个“演示工程”，而是一个可以持续迭代的实际项目基础。
