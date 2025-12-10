# Libman 项目 API 文档

本文档记录前端可直接调用的 HTTP 接口和常用的后端 common 类型说明。

目录

- API 概览（Controller 接口）
- common 类型字段说明与 JSON 示例
- AI 接口（聊天 / 流式）
- 图书封面上传与懒加载接口

---

## 第一部分：API 概览（Controller）

注意：多数接口返回统一包装 `Result<T>`，结构示例：

```json
{
  "status": "SUCCESS", // 或 "FAIL" / "UNLOGIN"
  "errorMessage": null,
  "data": null
}
```

### UserController (/user)

- POST `/user/login` — 表单登录

  - 参数（form）：`userName`、`password`
  - 返回：`Result<String>`（登录成功信息），登录会话保存在 server session

- POST `/user/register` — 注册

  - 参数（form）：`userName`、`password`
  - 返回：`Result<String>`

- GET `/user/info` — 获取当前登录用户信息（需携带 cookie/session）

  - 返回：`Result<UserInfo>` 或 `Result.fail("未登录")`

- POST `/user/avatar` — 上传头像（multipart/form-data）

  - 参数：`avatar` (file, png/jpeg, ≤2MB)，可选 `id` 用于指定用户
  - 返回：`Result<UserInfo>`（服务端会更新 session 中的 UserInfo）

- POST `/user/nickname` — 更新昵称

  - 参数（form）：`nickName`，可选 `id`
  - 返回：`Result<UserInfo>`

- POST `/user/password` — 修改密码
  - 参数（form）：`oldPassword`、`newPassword`，可选 `id`
  - 返回：`Result<UserInfo>`


### ViewController (/view)

这些接口返回视图模板（前端通常通过浏览器导航访问）：

- GET `/view/index` → `index`
- GET `/view/donate` → `donate`
- GET `/view/categories` → `categories`
- GET `/view/profile` → `profile`



### BookController (/book)

- POST `/book/getListByOffset` — 偏移分页

  - 请求 JSON: `{ "offset": <int>, "count": <int> }`
  - 返回：`Result<PageResult<BookInfo>>`

- POST `/book/getListByPage` — 页号分页

  - 请求 JSON: `PageRequest`（示例 `{ "currentPage":1, "pageSize":10 }`）
  - 返回：`Result<PageResult<BookInfo>>`

- GET `/book/getIndexPage` — 首页分页（默认第一页）
  - 请求参数：`pageSize` (query，可选，默认 10，取值 1-50)
  - 返回：`Result<PageResult<BookInfo>>`

- GET `/book/getLastPage` — 获取最后一页
  - 请求参数：`pageSize` (query，可选，默认 10，取值 1-50)
  - 返回：`Result<PageResult<BookInfo>>`

- POST `/book/addBook` — 添加图书（需登录）
  - 请求 JSON: `BookInfo`（后端自动写入 `donorId`）。成功触发 SSE `bookAdded`。
  - 返回：`Result<String>`

- POST `/book/addBookWithCover` — 添加图书并同时携带封面（multipart，需登录）
  - Content-Type: `multipart/form-data`
  - part1 名称 `book`：字符串形式 JSON，如：`{"bookName":"XX","author":"AA","price":39.9,"publish":"出版社","categoryIds":"[1,2]"}`
  - part2 名称 `file`：封面图片（可选，png/jpeg/webp，≤5MB）
  - 返回：`Result<BookInfo>`（包含持久化后、可能更新了 `coverUrl` 的对象）
  - 说明：未上传文件或文件为空时，`coverUrl` 默认为 `default.png`。

- POST `/{id}/cover` — 上传或更新指定图书封面（multipart，需登录）
  - URL 示例：`/book/123/cover`
  - Content-Type: `multipart/form-data`
  - 参数：`file` (图片 ≤5MB)
  - 返回：`Result<String>`（新封面文件名或完整 URL）

- GET `/{id}/cover` — 获取图书封面 URL（懒加载）
  - 返回：`Result<String>`，值可能是：`default.png` 或实际文件名；前端若非绝对路径需补成 `/covers/<文件名>`。
  - 缓存：服务端与 Redis 做封面 URL 缓存，命中则直接返回。

  - 请求 JSON: `BookInfo`（后端会写入 `donorId`）
  - 成功后服务器会广播 SSE 事件 `bookAdded`
  - 返回：`Result<String>`

- POST `/book/borrowBook` — 借阅（需登录）

  - 请求体为 JSON 裸整数：例如 `JSON.stringify(123)`（`@RequestBody Integer bookId`）
  - 返回：`Result<BookInfo>`，成功时触发 SSE `bookBorrowed`

- POST `/book/returnBook` — 归还（需登录）

  - 请求体为 JSON 裸整数，例如 `JSON.stringify(123)`
  - 返回：`Result<BookInfo>`，成功时触发 SSE `bookReturned`

- GET `/book/subscribe` — SSE 订阅端点（`text/event-stream`）

  - 事件示例：`bookAdded`、`bookBorrowed`、`bookReturned`、`bookUpdated`、`bookDeleted`

- POST `/book/isEnd` — 检测是否已加载完毕

  - 请求体：单个整数 `currentCount`（已加载数量）
  - 返回：`Result<BookLoadStatus>` (`isEnd`, `remaining`)

- POST `/book/getBooksByCategoryIds` — 根据分类 ID 列表获取书籍
  - 请求 JSON 示例：`{ "categoryIds": [1,2], "mode": 1 }`
  - 注意：控制器以 `String body` 接收原始 JSON 字符串，直接发送对象即可

### CategoryController (/category)

- GET `/category/list` — 获取所有分类
  - 返回：`Result<List<BookCategory>>`

### FeedbackController (/feedback)

- POST `/feedback/submit` — 提交用户反馈
  - 参数（form 或 x-www-form-urlencoded）：`content`（必填），`rating`（选填，1-5）
  - 认证：可匿名；若已登录将自动携带 `userId`
  - 返回：`Result<Feedback>`（包含新建记录）

- GET `/feedback/list` — 分页获取反馈列表
  - 参数（query）：`offset`（默认 0），`limit`（默认 20）
  - 返回：`Result<List<Feedback>>`

- GET `/feedback/{id}` — 获取单条反馈详情
  - 返回：`Result<Feedback>` 或 `Result.fail("记录不存在")`

`Feedback` 模型字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | Integer | 主键 |
| `userId` | Integer/null | 提交人（匿名为 null） |
| `content` | String | 反馈内容（必填） |
| `rating` | Integer/null | 评分（1-5，可空） |
| `createdAt` | Datetime | 创建时间 |

建表 SQL（位于 `docker/feedback_table.sql`）：

```sql
CREATE TABLE IF NOT EXISTS feedback (
  id INT PRIMARY KEY AUTO_INCREMENT,
  user_id INT NULL,
  content TEXT NOT NULL,
  rating INT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

## 第二部分：`common` 类型字段说明与 JSON 示例

下面给出 `src/main/java/com/example/demo/common` 中常用类型的字段说明及 JSON 传递示例，供前端和后端对齐使用格式。

1. `Result<T>`

- Java 字段：
  - `status` (`ResultStatus`)：响应状态（`SUCCESS` / `UNLOGIN` / `FAIL`），通常序列化为字符串。
  - `errorMessage`：失败时的错误描述字符串。
  - `data`：具体返回数据（根据接口而定）。

示例：

```json
{
  "status": "SUCCESS",
  "errorMessage": null,
  "data": {
    "total": 123,
    "records": []
  }
}
```

1. `ResultStatus`（枚举）

- 值与含义：`SUCCESS`(200)、`UNLOGIN`(-1)、`FAIL`(-2)。前端可直接匹配字符串值。

1. `PageRequest`（分页请求）

- 字段：`currentPage` (int, default 1), `pageSize` (int, default 8)

示例请求：

```json
{ "currentPage": 1, "pageSize": 10 }
```

1. `PageResult<T>`（分页响应）

- 字段：`total` (int), `records` (array)

示例响应：

```json
{
  "total": 250,
  "records": [{ "id": 1, "bookName": "示例书" }]
}
```

1. `OffsetRequest`

- 字段：`offset` (int, default 0), `count` (int, default 8)

示例请求：

```json
{ "offset": 24, "count": 8 }
```

1. `BookLoadStatus`

- 字段：`isEnd` (boolean), `remaining` (int)

示例返回：

```json
{ "status": "SUCCESS", "data": { "isEnd": false, "remaining": 120 } }
```

1. `BookStatus`（枚举）

- 值：`DELETED`(0)、`NORMAL`(1)、`FORBIDDEN`(2)、`NOTEXIST`(3)。
- 在 `BookInfo` 中通常以数字 `status` 字段出现（例如 `"status": 1`）。

示例片段：

```json
{ "id": 123, "bookName": "示例书", "status": 1 }
```

1. `CategoryQueryMode`

- 值：`INTERSECTION`(1)、`UNION`(2)。

示例请求：

```json
{ "categoryIds": [1, 2], "mode": 1 }
```

1. `Constants.SESSION_USER_KEY`

- 值：`"session_user_key"`（后端将登录用户保存在 session 中使用此键）。前端无需直接访问该键，但需在请求时携带 cookie 以保证 session 生效。

## 前端/后端数据交换要点

- 非文件请求统一使用 `Content-Type: application/json`，前端请用 `JSON.stringify` 序列化请求体。

- 对于接口接收裸整数（如 `/book/borrowBook`、`/book/returnBook`），请直接发送 `JSON.stringify(bookId)`（例如 `123`），而不是 `{ "bookId": 123 }`，当前服务端控制器按裸整数反序列化。

- `/book/getBooksByCategoryIds`：尽管服务端以 `String body` 接收，但前端可直接发送 JSON 对象（服务器会将其作为字符串读取并解析）。

- SSE (`/book/subscribe`)：事件 `e.data` 为字符串化 JSON，使用 `JSON.parse(e.data)` 解析后再处理。

---

## 第三部分：更新相关接口说明（BookController）

以下接口用于修改图书信息，前端可根据需要选择全量更新、部分更新或单字段更新。多数更新操作成功后，服务器会广播 SSE 事件 `bookUpdated`（或其他更具体的事件）。

- POST `/book/updateBookFull` — 全量更新（覆盖）
  - 请求 JSON：完整的 `BookInfo` 对象，必须包含 `id`。
  - 示例请求体：

  ```json
  {
    "id": 123,
    "bookName": "新名称",
    "author": "作者",
    "price": 19.9,
    "publish": "出版社",
    "status": 1,
    "tags": "[\"小说\", \"畅销\"]",
    "categoryIds": "[1,2]",
    "description": "详细描述"
  }
  ```

  - 返回：`Result<String>`（"更新成功" / 错误信息）

- POST `/book/patchBook` — 局部更新（PATCH 风格）
  - 请求 JSON：包含 `id` 和要更新的字段（只会更新传入的字段）。
  - 示例请求体（仅更新 tags 和 description）：

  ```json
  {
    "id": 123,
    "tags": "[\"热门\"]",
    "description": "更新后的简介"
  }
  ```

  - 返回：`Result<String>`（"更新成功" / 错误信息）。建议在前端进行合适的字段校验后再调用此接口。

- POST `/book/updateTags` — 单字段更新：tags
  - 请求 JSON：`{ "id": <int>, "tags": <string> }`。
  - 注意：后端当前以字符串形式保存 `tags`（数据库列为 JSON）。为避免反序列化错误，请将数组先序列化为字符串再发送（示例：`JSON.stringify(["小说"])`），或者使用 `/book/patchBook` 发送完整/部分对象并包含数组字段。
  - 示例：

  ```json
  { "id": 123, "tags": "[\"小说\",\"畅销\"]" }
  ```

  - 返回：`Result<String>`（"更新 tags 成功" / 错误信息）。成功时会触发 SSE `bookUpdated`。

- POST `/book/updateDescription` — 单字段更新：description
  - 请求 JSON：`{ "id": <int>, "description": "..." }`
  - 返回：`Result<String>`（"更新 description 成功" / 错误信息）。

- POST `/book/updateCategoryIds` — 单字段更新：categoryIds
  - 请求 JSON：`{ "id": <int>, "categoryIds": <string> }`，建议同 `tags` 一样发送字符串化的数组。
  - 示例：`{ "id": 123, "categoryIds": "[1,2]" }`
  - 返回：`Result<String>`（"更新 categoryIds 成功" / 错误信息）。

---

## 第五部分：AI 接口（聊天 / 流式）

### 1. 基本说明

AI 模块受配置开关控制：`application.properties` 中 `ai.enabled=true` 时启用；关闭时控制器不注册。

系统通过 `LibraryAiService` 封装底层模型（LangChain4j，同步调用），流式接口通过服务内部拆分字符模拟 token 推送。

### 2. 普通聊天接口

- POST `/ai/chat`
  - 请求 JSON：`{ "sessionId": "可选旧会话", "message": "用户输入" }`
  - 行为：若未提供 `sessionId` 或为空，后端生成一个新的；返回中一并提供。
  - 返回：

    ```json
    {
      "status": "SUCCESS",
      "sessionId": "xxxx-uuid",
      "reply": "模型回复（已清洗去除多余 UUID/前缀）"
    }
    ```
  - 错误：`message` 为空时返回 `{ "status":"FAIL", "errorMessage":"message 不能为空" }`

### 3. 流式聊天接口（SSE）

- POST `/ai/chat/stream`  （Response `Content-Type: text/event-stream`）
  - 请求 JSON：`{ "sessionId": "可选", "message": "用户输入" }`
  - 事件流格式：
    - `event: session` → 第一个事件，数据为规范化后的会话 id。
    - `event: token` → 多次出现，每次单个字符（Unicode code point），后端已进行了空白合并与换行限制过滤。
    - `event: done` → 最终完整文本（便于前端二次处理或缓存）。
    - `event: error` → 出错时返回，随后关闭连接。
  - 前端示例（浏览器）:
    ```js
    fetch('/ai/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: '介绍一下图书分类策略' })
    }).then(resp => {
      const reader = resp.body.getReader();
      const decoder = new TextDecoder('utf-8');
      // 这里也可以用 EventSource ，但由于是 POST，需要手动解析 SSE 帧
    });
    ```
  - 注意：SSE 自定义事件帧格式：`id:`（可选） + `event:` + `data:`，前端需按行解析；若使用 `EventSource` 只能 GET，因此此处采用 fetch + 读流形式。

### 4. 回复清洗与 Markdown 渲染

- 后端：`LibraryAiService.cleanReply` 去除随机 UUID / 会话 id 前缀，替换异常换行。
- 前端（`ai.html`）：使用 `marked.js + DOMPurify` 渲染，每 80ms 节流增量刷新；对非规范表格进行预处理 (`normalizeMalformedTables`)。
- 表格 GFM 支持：允许 `table/thead/tbody/tr/th/td` 白名单，避免 XSS。

### 5. 分类回退逻辑（工具函数）

- 当用户请求的分类不存在或为空时，Prompt 指令 + `searchBooksByCategoryWithFallback` 尝试同义/相近分类（如 “文学” → “艺术/文化/历史”）。
- 失败时优雅降级为提供热门/随机推荐而不是直接否定。

### 6. 常见错误与处理

- `message 不能为空`：请求体缺少或为空字符串。
- SSE 过程中网络断开：前端应提供重试按钮（使用旧 `sessionId` 继续）。
- 某些字符显示异常：前端可在接收 `token` 时做再过滤（当前后端已处理大部分空白合并）。

### 7. 安全注意事项

- DOMPurify 严格白名单渲染 Markdown，避免注入脚本。
- 建议对 AI 回复中出现的 URL 做额外校验再自动链接化。

### 8. 新增 AI 工具（面向模型的内部调用）

后端通过 `MapperTools` 暴露一组可供模型调用的检索工具，使 AI 在回答与图书相关的问题时能够获取最新的库数据。模型遇到指令（如“帮我找一本关于生物的书”）时，内部将自动选择合适工具，而无需前端显式调用。

当前可用工具（LangChain4j @Tool）：

| 方法 | 功能 | 典型触发语句示例 |
|------|------|------------------|
| `listBooksByOffset(offset, limit)` | 按偏移分页获取书籍 | “列出最近添加的几本书” |
| `countBooks()` | 获取总书籍数 | “图书馆共有多少书” |
| `getBookById(id)` / `getBooksByIds(ids)` | 根据单个 / 多个 ID 获取 | “ID 123 的书详情” |
| `listCategories()` | 获取所有分类 | “有哪些学科分类” |
| `searchBooksByCategoryWithFallback(name, limit)` | 按分类（带同义回退）搜索 | “找一些文学相关的书” |
| `searchBooksByTitle(title, limit)` | 按标题模糊搜索 | “有包含‘数学’的书吗” |
| `recommendRandomBooks(limit)` | 随机推荐若干书 | “随便推荐几本” |
| `searchBooksByAnyCategories(categoryNames, limit)` | 任意匹配多分类 | “给我一些历史或生物的书” |
| `summarizeLibrary()` | 输出总数与分类覆盖统计 | “图书馆整体概况” |
| `getBookDetailRich(id)` | 结构化书籍详细信息 | “详细介绍一下 ID 456 的书” |
| `getUserByName(userName)` | 查询捐赠者信息 | “这本书的捐赠者是谁” |

内部策略：

1. 当分类未命中时 `searchBooksByCategoryWithFallback` 会回退同义分类（例如 “小说” → “文学”）。
2. 标题搜索与分类搜索均在模型提示中加入列展示建议，模型会以 Markdown 表格形式输出（已在 continuation 补全逻辑中增强表格完整性）。
3. 若初次回答被检测为不完整（缺少句号或表格断行），会自动二次补全并合并到最终输出。

前端使用建议（可选增强）：

- 若希望显式唤起特定工具，可在用户输入前加系统前缀，例如：“(TOOL:searchBooksByTitle) 数学”。模型将更倾向调用对应方法。
- 过滤模型输出表格时，可检查列头是否与预期（书名/作者/出版社/价格/分类/状态/简介），不一致时可提示用户“请重述需求”。

Prompt 编写示例片段（内部）：

```text
请在需要查找书籍时优先使用提供的检索工具。输出结果使用规范 Markdown 表格：
|书名|作者|出版社|价格|分类|状态|内容简介|
表格之后给出1-2句总结。若首次生成未完成，请继续补全。
```


---
### 9. 外部网络搜索工具：Tavily

提供一个供 AI 模型使用的外部网络搜索工具（封装 Tavily Web Search API），用于回答需要最新互联网信息的问题。模型通过内部 @Tool `tavilySearch(query, maxResults, depth)` 调用。

环境配置：

| 项 | 说明 |
|----|------|
| 环境变量 `TAVILY_API_KEY` | Tavily 的 API Key，优先使用 |
| 属性 `tavily.api.key` | 可在 `application.properties` 中设置，作为备用 |
| 属性 `tavily.search.depth` | 默认 `basic`，可选 `advanced` |
| 属性 `tavily.max-results` | 默认返回前 5 条结果 |

示例调用（模型内部）：

```
请搜索：2025 年 Java 最新 LTS 版本的新特性，列出要点并引用来源链接。
```

模型执行逻辑：
1. 将用户请求作为 `query` 传给 Tavily。
2. 自动注入当前系统日期（`LocalDate.now()`）用于时间敏感回答展示。
3. 解析响应中的 `answer` 与 `results` 列表，截取内容摘要（约 160 字）。
4. 将结果格式化输出：日期 / 原始查询 / Answer / Top Sources（标题 + URL + 摘要）。

返回文本示例：

```
当前日期: 2025-11-23
原始查询: 2025 年 Java 最新 LTS 版本的新特性
搜索深度: basic 结果数量: 5
Answer: Java 21 是最新 LTS，引入虚拟线程、结构化并发等。
Top Sources:
1. Virtual Threads in Java
  https://example.com/virtual-threads
  摘要: 虚拟线程显著降低并发复杂度……
2. Structured Concurrency Overview
  https://example.com/structured-concurrency
  摘要: 结构化并发提供任务生命周期管理……
...
(数据来源：Tavily Web Search API)
```

错误与降级处理：
- 未配置密钥：返回提示“未配置 Tavily API Key”。
- HTTP 非 2xx：返回状态码与截断的响应体片段。
- 解析异常：返回错误消息与截断原始 JSON 片段。

使用建议：
- 仅在需要“最新”或“广域”信息时调用（例如技术趋势、新闻、政策），避免浪费配额。
- 问题可明确时间范围（例如“过去 6 个月”），模型仍会引用当前日期辅助回答。
- 若需更深爬取，可在提示中加入“使用 advanced 深度”使模型传参 `depth=advanced`。

安全注意事项：
- Tavily 返回的 URL 未经白名单过滤，前端若要跳转请使用 rel="noopener noreferrer" 且可展示来源域名让用户自行决定。
- 不要直接将搜索结果视为事实，答案段落仍建议附加“请进一步核对官方来源”。

---
### 10. 会话上下文存储（Redis + MySQL）

目标：对每位已登录用户，记录与 AI 的双向对话上下文。上下文保存在 Redis（带 TTL），到期自动落库 MySQL；同时限制总长度，超限则保留最近消息。

默认参数（可在 `application.properties` 调整）：
- `app.chat.context.max-chars=16000`：单用户会话 JSON 最大字符数（超出裁剪最旧消息）。
- `app.chat.redis.ttl-seconds=1800`：Redis 过期时间（秒），到期触发数据库落盘。
- `app.chat.redis.key-prefix=chat:ctx:`：Redis Key 前缀。

Redis 要求：开启 Keyspace 通知

```
CONFIG SET notify-keyspace-events Ex
```

MySQL 表结构（请先在数据库创建，已包含外键到 `user_info(id)`）：

```sql
CREATE TABLE `chat_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` INT NOT NULL,
  `context_json` LONGTEXT NOT NULL,
  `total_chars` INT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`),
  CONSTRAINT `fk_chat_history_user`
    FOREIGN KEY (`user_id`) REFERENCES `user_info`(`id`)
    ON DELETE CASCADE
    ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

如已创建无外键版本，可执行以下变更为带外键版本（若已有数据请先确认 `user_id` 全部合法）：

```sql
ALTER TABLE `chat_history`
  ADD CONSTRAINT `fk_chat_history_user`
  FOREIGN KEY (`user_id`) REFERENCES `user_info`(`id`)
  ON DELETE CASCADE
  ON UPDATE CASCADE;
```

说明：
- 采用 `ON DELETE CASCADE`，当用户被删除时自动清理其历史上下文；如需禁止删除可改为 `ON DELETE RESTRICT`。
- 字符集/排序规则与示例 `user_info` 一致（`utf8mb4_0900_ai_ci`）。

后端实现要点：
- 模型：`ChatHistory`；Mapper：`ChatHistoryMapper`（XML 见 `resources/mapper/ChatHistoryMapper.xml`）。
- 服务：`ChatContextService`
  - `appendMessage(userId, role, content)`: 读取 Redis → 追加消息 → 超限裁剪 → 写回 Redis（TTL 重置）。
  - 内存留存最近快照 `lastSnapshot` 用于过期回写（防止 Redis 过期后读不到值）。
  - `persistIfPresent(userId)`: 将最后快照 Upsert 到 MySQL。
- 监听器：`RedisKeyExpirationConfig` 订阅过期事件，匹配前缀后调用 `persistIfPresent`。
  - 需要 `RedisMessageListenerContainer` Bean（已提供）。

API：
- `POST /ai/context/append`：请求体 `{role:"user|assistant", content:"..."}`，需登录会话，保存到 Redis。
- `GET /ai/context/get`：返回当前会话 JSON 字符串（Redis 优先，DB 回退）。

工作流：
1. 用户与 AI 对话时，前端在每轮调用后向 `/ai/context/append` 上报当前轮消息（user 与 assistant 各一次）。
2. Redis 按 TTL 缓存上下文；过期时（Keyspace 事件）监听器将最近快照写入 MySQL。
3. 下次进入会话时若 Redis 没值，将自动从 DB 载入历史作为起点。

注意：
- 由于 Redis 过期事件不包含 Key 的值，本实现使用“最近快照”策略；若应用重启、快照丢失，仍可依赖上次已落库记录保底。
- 如需严格语义，可改为“写入时同步落库”，或引入定时任务在 TTL 前阈值主动落盘。

---

---

## 第六部分：图书封面上传与懒加载接口补充

### 1. 存储与访问路径

- 配置：`book.cover.base-dir` 物理存储目录；`book.cover.url-prefix` 逻辑访问前缀（映射为 `/covers/**`）。
- 默认封面：`default.png`（需放置在上述目录），数据库中 `cover_url` 列默认值为该文件名。

### 2. 相关字段

- `BookInfo.coverUrl`：保存文件名或完整 URL。若是文件名，前端展示时拼接 `/covers/<文件名>`。
- Redis 缓存：封面 URL 按书籍 ID 缓存，失效策略（例如 30 分钟）在 `BookCoverService` 中设定。

### 3. 接口汇总

| 接口 | 方法 | 描述 | 认证 | 请求体 | 返回 |
|------|------|------|------|--------|------|
| `/book/addBookWithCover` | POST multipart | 新增书籍 + 封面 | 需登录 | part `book` + part `file` | `Result<BookInfo>` |
| `/book/{id}/cover` | POST multipart | 上传/更新指定书籍封面 | 需登录 | part `file` | `Result<String>` |
| `/book/{id}/cover` | GET | 获取封面文件名/URL | 无 | 无 | `Result<String>` |

### 4. 前端使用示例（新增 + 展示）

新增：

```js
const form = new FormData();
form.append('book', JSON.stringify({ bookName:'XX', author:'AA', price:39.9, publish:'出版社', categoryIds:'[1]' }));
form.append('file', fileInput.files[0]);
const resp = await fetch('/book/addBookWithCover', { method:'POST', body: form });
const data = await resp.json();
```

展示（懒加载）：

```js
const r = await fetch(`/book/${bookId}/cover`);
const j = await r.json();
let url = j.data;
if (!/^https?:/.test(url)) url = '/covers/' + url; // 文件名 → 映射资源
img.src = url;
```

### 5. 封面加载失败回退

- 使用 `<img onerror="this.src='/covers/default.png'">` 处理 404。
- 前端可根据返回的 `status` 判断是否显示占位。

### 6. 安全与校验建议

- 后端已限制大小 ≤5MB；建议补充 MIME 验证（仅允许 image/png,image/jpeg,image/webp）。
- 可考虑生成缩略图（提高首页性能），并增加定期清理未引用旧文件的任务。

---

## 第四部分：模型字段说明（核心实体）

下面列出常用实体的字段说明和示例，便于前端与后端对齐。

### `BookInfo`（书籍信息）

- `id` (Integer): 唯一主键，自增。
- `bookName` (String): 书名。
- `author` (String): 作者名。
- `price` (BigDecimal): 价格。
- `publish` (String): 出版社或出版信息。
- `status` (Integer): 图书状态，见 `BookStatus` 枚举（0=DELETED,1=NORMAL,2=FORBIDDEN,3=NOTEXIST）。
- `tags` (String): 以 JSON 字符串形式保存的标签数组（例如 `"[\\"小说\\",\\"畅销\\"]"`）。
  - 注意：后端数据库列为 JSON，但当前 mapper/模型在服务层以 `String` 字段存储。前端可将标签数组 `JSON.stringify(tags)` 后发送，或使用 `patchBook` 发送数组并由服务端处理。
- `categoryIds` (String): 以 JSON 字符串保存的分类 ID 数组（例如 `"[1,2]"`）。同样建议使用字符串化形式或 `patchBook`。
- `description` (String): 描述 / 简介（可选，最长约 1024 字符）。
- `donorId` (Integer): 捐赠者用户 ID（添加时后端会自动写入）。
- `createTime` / `updateTime` (String/Datetime): 创建 / 更新时间戳。

示例 `BookInfo`：

```json
{
  "id": 123,
  "bookName": "示例书名",
  "author": "张三",
  "price": 39.9,
  "publish": "示例出版社",
  "status": 1,
  "tags": "[\\"小说\\",\\"悬疑\\"]",
  "categoryIds": "[1,3]",
  "description": "一本很有趣的书",
  "donorId": 45,
  "createTime": "2025-09-01T12:34:56",
  "updateTime": "2025-09-10T09:00:00"
}
```

### `UserInfo`（用户信息）

- `id` (Integer): 用户 ID。
- `userName` (String): 登录用户名。
- `nickName` (String): 显示昵称。
- `avatar` (String): 头像相对路径或 URL（上传后后端会在 `UserInfo` 中返回新的 avatar 字段）。
- `role` (String/Integer): 角色信息（视实现而定）。
- `createTime` / `updateTime` (String/Datetime): 时间戳。

示例 `UserInfo`：

```json
{
  "id": 45,
  "userName": "alice",
  "nickName": "Alice",
  "avatar": "/static/avatar/45.png",
  "role": "USER",
  "createTime": "2025-01-01T08:00:00",
  "updateTime": "2025-09-01T12:00:00"
}
```

---

提示与建议：

- 若希望前端发送原生数组而不先 stringify，建议在控制器层对 `tags` / `categoryIds` 做一层 `JsonNode` 检查与规范化（数组 → 字符串），我可以帮你把控制器补齐这部分以提高兼容性。
- 我也可以把这些接口和模型导出为 OpenAPI/Swagger YAML，便于自动生成 SDK 或在前端使用 TypeScript 类型。
 
---

## 第七部分：新增书籍管理 REST 接口（基于资源路径）

为提升前后端语义清晰度，系统新增一组更符合 REST 约定的接口（与原有 `/book/updateBookFull` 等 POST 接口并存，可渐进迁移）。这些接口统一使用路径参数表示资源 ID，区分全量与部分更新，并在服务器端自动处理：

1. 所有接口返回统一包装 `Result<T>`。
2. 需要登录且仅书籍捐赠者（拥有者）可修改；校验逻辑由 `bookService.isOwner(userId, bookId)` 完成。
3. PUT 全量更新时，未提供或为空的字符串字段会自动回退为旧值（特别是 `coverUrl`），避免写入 `null` 导致约束错误。
4. PATCH 仅对传入的非 null 字段执行更新，未出现的字段保持不变。
5. 更新成功后：
   - 刷新 Redis 缓存（`refreshBookCache(id)`）以使列表接口读取到最新数据。
   - 广播 SSE 事件 `bookUpdated`（前端可订阅实时刷新）。

### 1. 获取当前用户自己的书籍

`GET /book/my`

- 认证：需登录（session）
- 返回：`Result<List<BookInfo>>`（只包含当前登录用户捐赠的书籍）
- 用途：前端“管理我的书籍”页面初始列表。

### 2. 获取单本书籍详情（仅拥有者）

`GET /book/{id}`

- 认证：需登录并且是该书 `donorId`。
- 返回：`Result<BookInfo>`，附带分类名填充（`categoryNames`）。
- 失败：`Result.fail("未登录" | "无权限" | "不存在")`。

### 3. 全量更新书籍

`PUT /book/{id}`

- 认证：需登录且拥有者。
- 请求体：`BookUpdateRequest`（全字段 DTO）。字段为空字符串或 `null` 会自动回退旧值，避免覆盖为 `null`，示例：

```json
{
  "bookName": "新的书名",
  "author": "作者改动",
  "price": 49.9,
  "publish": "出版社",
  "status": 1,
  "tags": "[\"科幻\",\"经典\"]",
  "categoryIds": "[1,2]",
  "description": "新的简介",
  "coverUrl": "" // 空串表示不修改封面
}
```

- 返回：`Result<BookInfo>`（更新后的最新数据）。
- 封面字段处理：若 `coverUrl` 未提供或为空串，保留旧封面；若提供新文件需使用封面上传接口。 

### 4. 选择性更新书籍

`PATCH /book/{id}`

- 认证：需登录且拥有者。
- 请求体：`BookPatchRequest`（所有字段均可选）。仅非 null 字段会被更新。例如只更新简介与标签：

```json
{ "description": "补充更详细的简介", "tags": "[\"热销\"]" }
```

- 返回：`Result<BookInfo>`（最新数据）。
- 注意：不传的字段完全保持原值；如果需要清空简介，可显式传 `"description": ""`（将简介更新为空字符串）。

### 5. 上传或更新封面

`POST /book/{id}/cover` (multipart/form-data)

- part `file`: 新的封面图片（png/jpeg/webp ≤5MB）。
- 成功：返回 `Result<String>`（封面文件名或 URL），并刷新缓存 + 推送 `bookUpdated`。 
- 安全：仅拥有者可上传；未登录或无权限直接失败。

### 6. 懒加载封面

`GET /book/{id}/cover`

- 返回：`Result<String>`（文件名或完整 URL）。若为文件名，前端使用 `/covers/<文件名>` 拼接展示。
- 若文件缺失或为默认：可能返回 `default.png`。

### 7. DTO 字段说明

`BookUpdateRequest` 与 `BookPatchRequest` 字段集合（两者一致）：

| 字段 | 类型 | 说明 | 行为 |
|------|------|------|------|
| `bookName` | String | 书名 | PUT: 空串回退旧值；PATCH: 空串写入空值 |
| `author` | String | 作者 | 同上 |
| `price` | BigDecimal | 价格 | PUT 未提供回退旧值；PATCH 未提供不更新 |
| `publish` | String | 出版社 | 同上 |
| `status` | Integer | 状态 (0/1/2) | PUT 未提供回退旧值；PATCH 未提供不更新 |
| `tags` | String(JSON) | 标签数组字符串 | 建议使用 `JSON.stringify` 生成 |
| `categoryIds` | String(JSON) | 分类 ID 数组字符串 | 用于分类集合维护 |
| `description` | String | 简介 | PUT 空串回退旧；PATCH 空串写入空值 |
| `coverUrl` | String | 封面文件名或 URL | 通常由上传接口维护；PUT 空串/未传=保留旧值 |

### 8. 典型前端调用示例（PATCH）

```js
async function patchBook(id, payload) {
  const resp = await fetch(`/book/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload)
  });
  const data = await resp.json();
  if (data.status === 'SUCCESS') {
    // 更新本地 UI
  } else {
    console.error('更新失败', data.errorMessage);
  }
}

patchBook(123, { description: '新的简介', tags: JSON.stringify(['科幻']) });
```

### 9. Redis 缓存与实时刷新

每次成功 PUT/PATCH/封面上传：

- 服务端调用 `refreshBookCache(id)` 重建 `book:info:<id>` Hash（含 description/categoryIds/categoryNames/tags 等）。
- 通过 SSE 推送 `bookUpdated`，前端可监听后主动刷新对应卡片。示例：

```js
const es = new EventSource('/book/subscribe');
es.addEventListener('bookUpdated', e => {
  const book = JSON.parse(e.data);
  // 根据 book.id 局部更新 UI
});
```

### 10. 迁移建议

旧的 POST 更新接口（`/book/updateBookFull`, `/book/patchBook`, `/book/updateTags` 等）可逐步改为使用 PUT/PATCH 语义。前端在管理页面优先使用新接口；旧接口暂保留兼容，不建议新增功能继续扩展旧路径。

---
