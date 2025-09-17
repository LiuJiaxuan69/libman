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
}
