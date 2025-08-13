package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.common.BookStatus;
import com.example.demo.common.PageRequest;
import com.example.demo.common.PageResult;
import com.example.demo.mapper.BookInfoMapper;
import com.example.demo.mapper.BorrowHistoryMapper;
import com.example.demo.mapper.BorrowInfoMapper;
import com.example.demo.model.BookInfo;
import com.example.demo.model.BorrowInfo;

@Service
public class BookService {
    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Autowired
    private BorrowInfoMapper borrowInfoMapper;

    @Autowired
    private BorrowHistoryMapper borrowHistoryMapper;

    // 添加图书
    public boolean addBook(BookInfo bookInfo) {
        return bookInfoMapper.insertBook(bookInfo) > 0;
    }

    // 获取图书（分页获取）
    public PageResult<BookInfo> getBookListByPage(PageRequest pageRequest, Integer userId) {
        Integer count = bookInfoMapper.countBooks();
        List<BookInfo> books = bookInfoMapper.queryBookListByPage(pageRequest.getOffset(), pageRequest.getPageSize());
        for(BookInfo book: books) {
            book.setStatusCN(BookStatus.getNameByCode(book.getStatus()).getName());
            // 加工isBorrowedByMe字段
            if (userId != null) {
                book.setIsBorrowedByMe(borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(book.getId(), userId) != null);
            } else {
                book.setIsBorrowedByMe(false);
            }
        }
        return new PageResult<>(count, books);
    }

    // 借阅图书
    @Transactional
    public boolean borrowBook(Integer userId, Integer bookId) {
        // 检查图书是否存在
        BookInfo bookInfo = bookInfoMapper.queryBookById(bookId);
        if (bookInfo == null || bookInfo.getStatus() != BookStatus.NORMAL.getCode()) {
            return false; // 图书不存在或不可借阅
        }
        
        // 更新图书状态为借出
        bookInfoMapper.updateBookStatusById(bookId, BookStatus.FORBIDDEN.getCode());
        
        // 记录借阅信息
        BorrowInfo borrowInfo = new BorrowInfo();
        borrowInfo.setBookId(bookId);
        borrowInfo.setUserId(userId);
        if(borrowInfoMapper.insertBorrowInfo(borrowInfo) < 0) return false;
        borrowInfo = borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(bookId, userId);
        if(borrowHistoryMapper.insertBorrowInfo(borrowInfo) < 0) return false;
        return true;
    }

    // 归还图书
    public boolean returnBook(Integer userId, Integer bookId) {
        // 检查借阅记录是否存在
        BorrowInfo borrowInfo = borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(bookId, userId);
        if (borrowInfo == null) {
            return false; // 借阅记录不存在
        }

        // 更新图书状态为可借阅
        bookInfoMapper.updateBookStatusById(bookId, BookStatus.NORMAL.getCode());

        // 删除借阅记录
        if (borrowInfoMapper.deleteBorrowInfo(borrowInfo.getBookId(), borrowInfo.getUserId()) < 0) return false;
        return true;
    }
}
