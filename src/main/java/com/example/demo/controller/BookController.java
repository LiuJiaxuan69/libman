package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.BookLoadStatus;
import com.example.demo.common.BookStatus;
import com.example.demo.common.Constants;
import com.example.demo.common.OffsetRequest;
import com.example.demo.common.PageRequest;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.model.BookInfo;
import com.example.demo.model.UserInfo;
import com.example.demo.service.BookService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import com.example.demo.dto.BookUpdateRequest;
import com.example.demo.dto.BookPatchRequest;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;


@Slf4j
@RequestMapping("/book")
@RestController
public class BookController {
    @Autowired
    private BookService bookService;
    @Autowired
    private com.example.demo.mapper.BookInfoMapper bookInfoMapper;

    @Autowired
    private com.example.demo.service.SseService sseService;
    @Autowired
    private com.example.demo.service.BookCoverService bookCoverService;

    // 空值/空串回退：如果新值为 null 或空串，则返回旧值
    private String emptyFallback(String newVal, String oldVal) {
        if (newVal == null) return oldVal;
        if (newVal.trim().isEmpty()) return oldVal;
        return newVal;
    }

    // 分页获取书籍信息
    @RequestMapping("/getListByPage")
    public Result<PageResult<BookInfo>> getListByPage(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
            if (userInfo != null) {
                userId = userInfo.getId();
            }
        }
        // 调用 service 层方法获取数据
        PageResult<BookInfo> pageResult = bookService.getBookListByPage(pageRequest, userId);
        return Result.success(pageResult);
    }

    // 上传 / 更新 书籍封面（Multipart）
    @PostMapping(value="/{id}/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadCover(@PathVariable("id") Integer bookId,
                                      @RequestParam(value="file", required=false) MultipartFile file) {
        try {
            // 权限：仅捐赠者可更新封面
            // 读取当前登录用户
            var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
            if (session == null) return Result.fail("未登录");
            com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
            if (user == null) return Result.fail("未登录");
            if (!bookService.isOwner(user.getId(), bookId)) return Result.fail("无权限");
            String url = bookCoverService.uploadCover(bookId, file);
            // 刷新书籍缓存（封面变化）
            bookService.refreshBookCache(bookId);
            // 推送更新事件
            BookInfo updated = bookInfoMapper.queryBookById(bookId);
            if (updated != null) {
                sseService.sendEvent("bookUpdated", updated);
            }
            return Result.success(url);
        } catch (Exception ex) {
            log.error("uploadCover error", ex);
            return Result.fail("上传封面失败" + (ex.getMessage() == null ? "" : (":" + ex.getMessage())));
        }
    }

    // 获取书籍封面URL（懒加载）
    @GetMapping("/{id}/cover")
    public Result<String> getCover(@PathVariable("id") Integer bookId) {
        try {
            String url = bookCoverService.getCoverUrl(bookId);
            return Result.success(url);
        } catch (Exception ex) {
            log.error("getCover error", ex);
            return Result.fail("获取封面失败");
        }
    }

    // 按照偏移量分页获取书籍信息
    @RequestMapping("/getListByOffset")
    public Result<PageResult<BookInfo>> getBookListByOffset(@RequestBody OffsetRequest offsetRequest, HttpServletRequest request) {
        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
            if (userInfo != null) {
                userId = userInfo.getId();
            }
        }
        // 调用 service 层方法获取数据
        PageResult<BookInfo> pageResult = bookService.getBookListByOffset(offsetRequest.getOffset(), offsetRequest.getCount(), userId);
        return Result.success(pageResult);
    }
    // 获取首页分页信息
    @RequestMapping("/getIndexPage")
    public Result<PageResult<BookInfo>> getIndexPage(HttpServletRequest request, Integer pageSize) {
         if (pageSize == null || pageSize <= 0 || pageSize > 50) {
             pageSize = 10;
         }
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrentPage(1);
        pageRequest.setPageSize(pageSize);
        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
            if (userInfo != null) {
                userId = userInfo.getId();
            }
        }
        // 调用 service 层方法获取数据
        PageResult<BookInfo> pageResult = bookService.getBookListByPage(pageRequest, userId);
        return Result.success(pageResult);
    }

    // 获取最后一页分页信息
    @RequestMapping("/getLastPage")
    public Result<PageResult<BookInfo>> getLastPage(HttpServletRequest request, Integer pageSize) {
         if (pageSize == null || pageSize <= 0 || pageSize > 50) {
             pageSize = 10;
         }
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrentPage(Integer.MAX_VALUE);
        pageRequest.setPageSize(pageSize);
        Integer userId = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
            if (userInfo != null) {
                userId = userInfo.getId();
            }
        }
        // 调用 service 层方法获取数据
        PageResult<BookInfo> pageResult = bookService.getBookListByPage(pageRequest, userId);
        return Result.success(pageResult);
    }

    // 添加图书
    @RequestMapping("/addBook")
    public Result<String> addBook(@RequestBody BookInfo bookInfo, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        UserInfo userInfo = (UserInfo)session.getAttribute(Constants.SESSION_USER_KEY);
        bookInfo.setDonorId(userInfo.getId());
        log.info("添加图书，图书信息：{}", bookInfo);
        boolean success = bookService.addBook(bookInfo);
        if (success) {
            log.info("添加图书成功");
            // 广播新增图书事件（客户端可更新本地缓存）
            sseService.sendEvent("bookAdded", bookInfo);
            return Result.success("添加图书成功");
        } else {
            log.error("添加图书失败");
            return Result.fail("添加图书失败");
        }
    }

    // 添加图书（携带封面） multipart：部分字段在 JSON part "book", 封面在 file
    @PostMapping(value = "/addBookWithCover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<BookInfo> addBookWithCover(@RequestParam("book") String bookJson,
                                             @RequestParam(value="file", required=false) MultipartFile file,
                                             HttpServletRequest request) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            BookInfo bookInfo = mapper.readValue(bookJson, BookInfo.class);
            HttpSession session = request.getSession(false);
            UserInfo userInfo = (UserInfo)session.getAttribute(Constants.SESSION_USER_KEY);
            bookInfo.setDonorId(userInfo.getId());
            if (bookInfo.getCoverUrl() == null || bookInfo.getCoverUrl().isBlank()) {
                bookInfo.setCoverUrl("default.png");
            }
            boolean success = bookService.addBook(bookInfo);
            if (!success) return Result.fail("添加图书失败");
            // 上传封面（如果有）
            if (file != null && !file.isEmpty()) {
                if (file.getSize() > 5 * 1024 * 1024) {
                    return Result.fail("封面超过5MB限制");
                }
                String url = bookCoverService.uploadCover(bookInfo.getId(), file); // 更新 coverUrl
                bookInfo.setCoverUrl(url);
            }
            BookInfo fresh = bookInfoMapper.queryBookById(bookInfo.getId());
            sseService.sendEvent("bookAdded", fresh);
            return Result.success(fresh);
        } catch (Exception ex) {
            log.error("addBookWithCover error", ex);
            return Result.fail("服务器错误");
        }
    }

    // 借阅图书
    @RequestMapping("/borrowBook")
    public Result<BookInfo> borrowBook(@RequestBody Integer bookId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("用户未登录");
        }
        UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (userInfo == null) {
            return Result.fail("用户未登录");
        }
        log.info("借阅图书，用户ID：{}，图书ID：{}", userInfo.getId(), bookId);
        BookStatus status = bookService.borrowBook(userInfo.getId(), bookId);
        if (status == BookStatus.NORMAL) {
            log.info("借阅图书成功");
            BookInfo updated = bookInfoMapper.queryBookById(bookId);
            sseService.sendEvent("bookBorrowed", updated);
            // ensure cache updated in service already, but return authoritative BookInfo
            return Result.success(updated);
        } else {
            if (status == BookStatus.FORBIDDEN) {
                log.error("借阅图书失败，图书已被借出");
                return Result.fail("借阅图书失败，图书已被借出");
            } else if (status == BookStatus.DELETED) {
                log.error("借阅图书失败，图书无效");
                return Result.fail("借阅图书失败，图书无效");
            } else if (status == BookStatus.NOTEXIST) {
                log.error("借阅图书失败，图书不存在");
                return Result.fail("借阅图书失败，图书不存在");
            } else {
                log.error("借阅图书失败，未知错误");
                return Result.fail("借阅图书失败，未知错误");
            }
        }
    }

    // 归还图书
    @RequestMapping("/returnBook")
    public Result<BookInfo> returnBook(@RequestBody Integer bookId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("用户未登录");
        }
        UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (userInfo == null) {
            return Result.fail("用户未登录");
        }
        log.info("归还图书，用户ID：{}，图书ID：{}", userInfo.getId(), bookId);
        boolean success = bookService.returnBook(userInfo.getId(), bookId);
        if (success) {
            log.info("归还图书成功");
            BookInfo updated = bookInfoMapper.queryBookById(bookId);
            sseService.sendEvent("bookReturned", updated);
            return Result.success(updated);
        } else {
            log.error("归还图书失败");
            return Result.fail("归还图书失败");
        }
    }

    // SSE 订阅端点
    @GetMapping("/subscribe")
    public SseEmitter subscribeToBookUpdates() {
        return sseService.createEmitter();
    }

    // 检测图书是否加载完毕
    @RequestMapping("/isEnd")
    public Result<BookLoadStatus> isEnd(@RequestBody Integer currentCount) {
        log.info("检测图书是否加载完毕，当前已加载图书数量：{}", currentCount);
        boolean isEnd = currentCount >= bookService.getBookCount();
        log.info("图书加载完毕状态：{}", isEnd);
        BookLoadStatus status = new BookLoadStatus(isEnd, bookService.getBookCount() - currentCount);
        return Result.success(status);
    }

    // 根据 JSON 格式的分类 ID 列表获取对应的书籍列表，支持 mode：1=交集，2=并集
    @RequestMapping("/getBooksByCategoryIds")
    public Result<java.util.List<BookInfo>> getBooksByCategoryIds(@RequestBody String body) {
        log.info("根据 JSON 格式的分类 ID 列表获取对应的书籍列表，请求体：{}", body);
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> map = mapper.readValue(body, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            String categoryIdsJson = mapper.writeValueAsString(map.getOrDefault("categoryIds", "[]"));
            int mode = 1;
            Object mobj = map.get("mode");
            if (mobj != null) {
                try { mode = Integer.parseInt(String.valueOf(mobj)); } catch (Exception ignore) {}
            }
            java.util.List<BookInfo> books = bookService.getBooksByCategoryIds(categoryIdsJson, mode);
            log.info("获取到的书籍列表：{}", books);
            return Result.success(books);
        } catch (Exception ex) {
            log.error("解析 getBooksByCategoryIds 请求体失败", ex);                                              
            return Result.fail("请求格式错误");
        }
    }

    // ============ 新增/更新相关接口 ============

    // 全量更新一本书（整本替换）
    @RequestMapping("/updateBookFull")
    public Result<String> updateBookFull(@RequestBody BookInfo bookInfo) {
        if (bookInfo == null || bookInfo.getId() == null) {
            return Result.fail("参数错误");
        }
        // 权限校验：只能修改自己的书
        var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
        if (session == null) return Result.fail("未登录");
        com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
        if (user == null) return Result.fail("未登录");
        if (!bookService.isOwner(user.getId(), bookInfo.getId())) return Result.fail("无权限");
        try {
            boolean ok = bookService.updateBookFull(bookInfo);
            if (ok) {
                BookInfo updated = bookInfoMapper.queryBookById(bookInfo.getId());
                sseService.sendEvent("bookUpdated", updated);
                return Result.success("更新成功");
            } else {
                return Result.fail("更新失败");
            }
        } catch (Exception ex) {
            log.error("updateBookFull error", ex);
            return Result.fail("服务器错误");
        }
    }

    // PATCH 风格：选择性更新（只更新非 null 字段）
    @RequestMapping("/patchBook")
    public Result<String> patchBook(@RequestBody BookInfo bookInfo) {
        if (bookInfo == null || bookInfo.getId() == null) {
            return Result.fail("参数错误");
        }
        var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
        if (session == null) return Result.fail("未登录");
        com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
        if (user == null) return Result.fail("未登录");
        if (!bookService.isOwner(user.getId(), bookInfo.getId())) return Result.fail("无权限");
        try {
            boolean ok = bookService.updateBookSelective(bookInfo);
            if (ok) {
                BookInfo updated = bookInfoMapper.queryBookById(bookInfo.getId());
                sseService.sendEvent("bookUpdated", updated);
                return Result.success("更新成功");
            } else {
                return Result.fail("更新失败");
            }
        } catch (Exception ex) {
            log.error("patchBook error", ex);
            return Result.fail("服务器错误");
        }
    }

    // 单字段：更新 tags（tags 可以是 JSON 数组或字符串）
    @RequestMapping("/updateTags")
    public Result<String> updateTags(@RequestBody java.util.Map<String, Object> body) {
        try {
            Integer id = body.get("id") == null ? null : Integer.parseInt(String.valueOf(body.get("id")));
            if (id == null) return Result.fail("id 不能为空");
            var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
            if (session == null) return Result.fail("未登录");
            com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
            if (user == null) return Result.fail("未登录");
            if (!bookService.isOwner(user.getId(), id)) return Result.fail("无权限");
            Object tagsObj = body.get("tags");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String tagsStr;
            if (tagsObj == null) {
                tagsStr = null;
            } else if (tagsObj instanceof String) {
                tagsStr = (String) tagsObj;
            } else {
                tagsStr = mapper.writeValueAsString(tagsObj);
            }
            boolean ok = bookService.updateTags(id, tagsStr);
            if (ok) {
                BookInfo updated = bookInfoMapper.queryBookById(id);
                sseService.sendEvent("bookUpdated", updated);
                return Result.success("更新 tags 成功");
            } else {
                return Result.fail("更新 tags 失败");
            }
        } catch (Exception ex) {
            log.error("updateTags error", ex);
            return Result.fail("服务器错误");
        }
    }

    // 单字段：更新 description
    @RequestMapping("/updateDescription")
    public Result<String> updateDescription(@RequestBody java.util.Map<String, Object> body) {
        try {
            Integer id = body.get("id") == null ? null : Integer.parseInt(String.valueOf(body.get("id")));
            if (id == null) return Result.fail("id 不能为空");
            var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
            if (session == null) return Result.fail("未登录");
            com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
            if (user == null) return Result.fail("未登录");
            if (!bookService.isOwner(user.getId(), id)) return Result.fail("无权限");
            Object descObj = body.get("description");
            String description = descObj == null ? null : String.valueOf(descObj);
            boolean ok = bookService.updateDescription(id, description);
            if (ok) {
                BookInfo updated = bookInfoMapper.queryBookById(id);
                sseService.sendEvent("bookUpdated", updated);
                return Result.success("更新 description 成功");
            } else {
                return Result.fail("更新 description 失败");
            }
        } catch (Exception ex) {
            log.error("updateDescription error", ex);
            return Result.fail("服务器错误");
        }
    }

    // 单字段：更新 categoryIds（接收数组或 JSON 字符串）
    @RequestMapping("/updateCategoryIds")
    public Result<String> updateCategoryIds(@RequestBody java.util.Map<String, Object> body) {
        try {
            Integer id = body.get("id") == null ? null : Integer.parseInt(String.valueOf(body.get("id")));
            if (id == null) return Result.fail("id 不能为空");
            var session = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes() instanceof org.springframework.web.context.request.ServletRequestAttributes sra ? sra.getRequest().getSession(false) : null;
            if (session == null) return Result.fail("未登录");
            com.example.demo.model.UserInfo user = (com.example.demo.model.UserInfo) session.getAttribute(com.example.demo.common.Constants.SESSION_USER_KEY);
            if (user == null) return Result.fail("未登录");
            if (!bookService.isOwner(user.getId(), id)) return Result.fail("无权限");
            Object catsObj = body.get("categoryIds");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String catsJson;
            if (catsObj == null) {
                catsJson = null;
            } else if (catsObj instanceof String) {
                catsJson = (String) catsObj;
            } else {
                catsJson = mapper.writeValueAsString(catsObj);
            }
            boolean ok = bookService.updateCategoryIds(id, catsJson);
            if (ok) {
                BookInfo updated = bookInfoMapper.queryBookById(id);
                sseService.sendEvent("bookUpdated", updated);
                return Result.success("更新 categoryIds 成功");
            } else {
                return Result.fail("更新 categoryIds 失败");
            }
        } catch (Exception ex) {
            log.error("updateCategoryIds error", ex);
            return Result.fail("服务器错误");
        }
    }

    // ============ 用户自有书籍管理相关 REST 风格接口 ============

    // 获取当前登录用户捐赠的书籍列表
    @GetMapping("/my")
    public Result<List<BookInfo>> myBooks(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return Result.fail("未登录");
        UserInfo u = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (u == null) return Result.fail("未登录");
        List<BookInfo> list = bookInfoMapper.queryBooksByDonor(u.getId());
        // 填充分类名
        bookService.fillCategoryNamesForBooks(list);
        return Result.success(list);
    }

    // 获取单本书详情（仅限拥有者）
    @GetMapping("/{id}")
    public Result<BookInfo> getBookDetail(@PathVariable("id") Integer id, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return Result.fail("未登录");
        UserInfo u = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (u == null) return Result.fail("未登录");
        if (!bookService.isOwner(u.getId(), id)) return Result.fail("无权限");
        BookInfo b = bookInfoMapper.queryBookById(id);
        if (b == null) return Result.fail("不存在");
        bookService.fillCategoryNamesForBooks(java.util.List.of(b));
        return Result.success(b);
    }

    // REST: PUT 全量更新（与 /updateBookFull 类似，但路径式 + 权限）
    @PutMapping("/{id}")
    public Result<BookInfo> putFull(@PathVariable("id") Integer id, @RequestBody BookUpdateRequest body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return Result.fail("未登录");
        UserInfo u = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (u == null) return Result.fail("未登录");
        if (!bookService.isOwner(u.getId(), id)) return Result.fail("无权限");
        if (body == null) return Result.fail("参数错误");
        // 保留原 donorId，避免覆盖/置空
        BookInfo old = bookInfoMapper.queryBookById(id);
        if (old == null) return Result.fail("不存在");
        BookInfo info = new BookInfo();
        info.setId(id);
        info.setDonorId(old.getDonorId() == null ? u.getId() : old.getDonorId());
        // 若传入字段为 null 或空串，使用旧值，避免 NOT NULL 约束失败 & 保留原封面
        info.setBookName(emptyFallback(body.getBookName(), old.getBookName()));
        info.setAuthor(emptyFallback(body.getAuthor(), old.getAuthor()));
        info.setPrice(body.getPrice() == null ? old.getPrice() : body.getPrice());
        info.setPublish(emptyFallback(body.getPublish(), old.getPublish()));
        info.setDescription(emptyFallback(body.getDescription(), old.getDescription()));
        info.setCategoryIds(emptyFallback(body.getCategoryIds(), old.getCategoryIds()));
        info.setTags(emptyFallback(body.getTags(), old.getTags()));
        info.setStatus(body.getStatus() == null ? old.getStatus() : body.getStatus());
        info.setCoverUrl(emptyFallback(body.getCoverUrl(), old.getCoverUrl()));
        boolean ok = bookService.updateBookFull(info);
        if (!ok) return Result.fail("更新失败");
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        sseService.sendEvent("bookUpdated", fresh);
        return Result.success(fresh);
    }

    // REST: PATCH 选择性更新
    @PatchMapping("/{id}")
    public Result<BookInfo> patch(@PathVariable("id") Integer id, @RequestBody BookPatchRequest body, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return Result.fail("未登录");
        UserInfo u = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (u == null) return Result.fail("未登录");
        if (!bookService.isOwner(u.getId(), id)) return Result.fail("无权限");
        if (body == null) return Result.fail("参数错误");
        BookInfo old = bookInfoMapper.queryBookById(id);
        if (old == null) return Result.fail("不存在");
        BookInfo info = new BookInfo();
        info.setId(id);
        info.setDonorId(old.getDonorId() == null ? u.getId() : old.getDonorId()); // 不更改 donorId
        info.setBookName(body.getBookName());
        info.setAuthor(body.getAuthor());
        info.setPrice(body.getPrice());
        info.setPublish(body.getPublish());
        info.setDescription(body.getDescription());
        info.setCategoryIds(body.getCategoryIds());
        info.setTags(body.getTags());
        info.setStatus(body.getStatus());
        info.setCoverUrl(body.getCoverUrl());
        boolean ok = bookService.updateBookSelective(info);
        if (!ok) return Result.fail("更新失败");
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        sseService.sendEvent("bookUpdated", fresh);
        return Result.success(fresh);
    }
}
