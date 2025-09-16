# Libman 项目 API 文档

本文档记录前端可直接调用的 HTTP 接口和常用的后端 common 类型说明。

目录
- API 概览（Controller 接口）
- common 类型字段说明与 JSON 示例


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

- GET `/view/login` → `auth/login`
- GET `/view/register` → `auth/register`
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

- POST `/book/getIndexPage` — 首页分页（默认第一页、pageSize=10）

- POST `/book/getLastPage` — 获取最后一页

- POST `/book/addBook` — 添加图书（需登录）
  - 请求 JSON: `BookInfo`（后端会写入 `donorId`）
  - 成功后服务器会广播 SSE 事件 `bookAdded`

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


---

## 第二部分：`common` 类型字段说明与 JSON 示例

下面给出 `src/main/java/com/example/demo/common` 中常用类型的字段说明及 JSON 传递示例，供前端和后端对齐使用格式。

1) `Result<T>`
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

2) `ResultStatus`（枚举）
- 值与含义：`SUCCESS`(200)、`UNLOGIN`(-1)、`FAIL`(-2)。前端可直接匹配字符串值。

3) `PageRequest`（分页请求）
- 字段：`currentPage` (int, default 1), `pageSize` (int, default 8)

示例请求：

```json
{ "currentPage": 1, "pageSize": 10 }
```

4) `PageResult<T>`（分页响应）
- 字段：`total` (int), `records` (array)

示例响应：

```json
{
  "total": 250,
  "records": [ { "id": 1, "bookName": "示例书" } ]
}
```

5) `OffsetRequest`
- 字段：`offset` (int, default 0), `count` (int, default 8)

示例请求：

```json
{ "offset": 24, "count": 8 }
```

6) `BookLoadStatus`
- 字段：`isEnd` (boolean), `remaining` (int)

示例返回：

```json
{ "status": "SUCCESS", "data": { "isEnd": false, "remaining": 120 } }
```

7) `BookStatus`（枚举）
- 值：`DELETED`(0)、`NORMAL`(1)、`FORBIDDEN`(2)、`NOTEXIST`(3)。
- 在 `BookInfo` 中通常以数字 `status` 字段出现（例如 `"status": 1`）。

示例片段：

```json
{ "id": 123, "bookName": "示例书", "status": 1 }
```

8) `CategoryQueryMode`
- 值：`INTERSECTION`(1)、`UNION`(2)。

示例请求：

```json
{ "categoryIds": [1,2], "mode": 1 }
```

9) `Constants.SESSION_USER_KEY`
- 值：`"session_user_key"`（后端将登录用户保存在 session 中使用此键）。前端无需直接访问该键，但需在请求时携带 cookie 以保证 session 生效。


## 前端/后端数据交换要点

- 非文件请求统一使用 `Content-Type: application/json`，前端请用 `JSON.stringify` 序列化请求体。

- 对于接口接收裸整数（如 `/book/borrowBook`、`/book/returnBook`），请直接发送 `JSON.stringify(bookId)`（例如 `123`），而不是 `{ "bookId": 123 }`，当前服务端控制器按裸整数反序列化。

- `/book/getBooksByCategoryIds`：尽管服务端以 `String body` 接收，但前端可直接发送 JSON 对象（服务器会将其作为字符串读取并解析）。

- SSE (`/book/subscribe`)：事件 `e.data` 为字符串化 JSON，使用 `JSON.parse(e.data)` 解析后再处理。


---

如果你希望我把 `model` 下的实体（如 `BookInfo`、`UserInfo`）字段也自动列出来并追加到 README，或把整个 API 转成 OpenAPI/Swagger YAML，我可以继续完成（请选择下一步）。

## 第二部分：`common` 类型字段说明与 JSON 示例

代码中注释写的很详细(≧▽≦)！