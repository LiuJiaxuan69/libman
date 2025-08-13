package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Constants;
import com.example.demo.common.PageRequest;
import com.example.demo.common.PageResult;
import com.example.demo.common.Result;
import com.example.demo.model.BookInfo;
import com.example.demo.model.UserInfo;
import com.example.demo.service.BookService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RequestMapping("/book")
@RestController
public class BookController {
    @Autowired
    private BookService bookService;

    // 分页获取书籍信息
    @RequestMapping("/getListByPage")
    public Result<PageResult<BookInfo>> getListByPage(@RequestBody PageRequest pageRequest, HttpServletRequest request) {
        log.info("获取图书列表，当前页：{}, 每页记录数：{}", pageRequest.getCurrentPage(), pageRequest.getPageSize());
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
        log.info("获取图书列表成功，详细信息如下：{}", pageResult);
        return Result.success(pageResult);
    }
    // 获取首页分页信息
    @RequestMapping("/getIndexPage")
    public Result<PageResult<BookInfo>> getIndexPage(HttpServletRequest request) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrentPage(1);
        pageRequest.setPageSize(10);
        log.info("获取首页图书列表，当前页：{}, 每页记录数：{}", pageRequest.getCurrentPage(), pageRequest.getPageSize());
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
        log.info("获取首页图书列表成功，详细信息如下：{}", pageResult);
        return Result.success(pageResult);
    }

    // 获取最后一页分页信息
    @RequestMapping("/getLastPage")
    public Result<PageResult<BookInfo>> getLastPage(HttpServletRequest request) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setCurrentPage(Integer.MAX_VALUE);
        pageRequest.setPageSize(10);
        log.info("获取最后一页图书列表，当前页：{}, 每页记录数：{}", pageRequest.getCurrentPage(), pageRequest.getPageSize());
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
        log.info("获取最后一页图书列表成功，详细信息如下：{}", pageResult);
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
            return Result.success("添加图书成功");
        } else {
            log.error("添加图书失败");
            return Result.fail("添加图书失败");
        }
    }

    // 借阅图书
    @RequestMapping("/borrowBook")
    public Result<String> borrowBook(@RequestBody Integer bookId, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Result.fail("用户未登录");
        }
        UserInfo userInfo = (UserInfo) session.getAttribute(Constants.SESSION_USER_KEY);
        if (userInfo == null) {
            return Result.fail("用户未登录");
        }
        log.info("借阅图书，用户ID：{}，图书ID：{}", userInfo.getId(), bookId);
        boolean success = bookService.borrowBook(userInfo.getId(), bookId);
        if (success) {
            log.info("借阅图书成功");
            return Result.success("借阅图书成功");
        } else {
            log.error("借阅图书失败");
            return Result.fail("借阅图书失败");
        }
    }

    // 归还图书
    @RequestMapping("/returnBook")
    public Result<String> returnBook(@RequestBody Integer bookId, HttpServletRequest request) {
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
            return Result.success("归还图书成功");
        } else {
            log.error("归还图书失败");
            return Result.fail("归还图书失败");
        }
    }

}
