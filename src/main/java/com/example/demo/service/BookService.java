package com.example.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.IdGenerator;

import com.example.demo.common.BookStatus;
import com.example.demo.common.PageRequest;
import com.example.demo.common.PageResult;
import com.example.demo.common.TimeBase62UUIDGenerator;
import com.example.demo.config.RedisConfig;
import com.example.demo.mapper.BookInfoMapper;
import com.example.demo.mapper.BorrowHistoryMapper;
import com.example.demo.mapper.BorrowInfoMapper;
import com.example.demo.model.BookInfo;
import com.example.demo.model.BorrowInfo;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

@Service
public class BookService {
    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Autowired
    private BorrowInfoMapper borrowInfoMapper;

    @Autowired
    private BorrowHistoryMapper borrowHistoryMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String BOOK_INDEX_KEY = "book:index";
    private static final String BOOK_INFO_KEY_PREFIX = "book:info:"; // + bookId
    private static final long BOOK_EXPIRE_MINUTES = 10; // Hash过期时间
    
    private static final Logger logger = Logger.getLogger(BookService.class.getName());
    
    private int index = 0;

    /** 启动时缓存ID→索引映射 */
    @PostConstruct
    public void initRedisIndex() {

        List<Integer> allBooks = bookInfoMapper.queryAllBookIds(); // 查询所有书籍ID
        for (Integer bookId : allBooks) {
            redisTemplate.opsForZSet().add(BOOK_INDEX_KEY, bookId, index++);
        }
    }

    // 添加图书
    // BookService.java
public boolean addBook(BookInfo bookInfo) {
    // 如果传入的 bookInfo 没有 ID，就生成一个
    if (bookInfo.getId() == null) {
        String generatedId = TimeBase62UUIDGenerator.generate(); // 返回 String
        // 如果 BookInfo 的 id 是 Integer 类型，可以用 hashCode 或者自行修改类型为 String
        bookInfo.setId(generatedId.hashCode()); 
    }

    logger.info("Adding book: " + bookInfo);

    // 先存入数据库，保证数据库成功后再更新 Redis
    boolean inserted = bookInfoMapper.insertBook(bookInfo) > 0;

    if (inserted) {
        // 更新 Redis 索引
        redisTemplate.opsForZSet().add(BOOK_INDEX_KEY, bookInfo.getId(), index++);
    }

    return inserted;
}


    /** 将BookInfo存入Redis Hash */
    private void saveBookToRedis(BookInfo book) {
        String hashKey = BOOK_INFO_KEY_PREFIX + book.getId();
        Map<String, Object> map = new HashMap<>();
        map.put("id", book.getId());
        map.put("bookName", book.getBookName());
        map.put("author", book.getAuthor());
        map.put("publish", book.getPublish());
        map.put("price", book.getPrice());
        map.put("status", book.getStatus());
        map.put("isBorrowedByMe", book.getIsBorrowedByMe());
        redisTemplate.opsForHash().putAll(hashKey, map);
        redisTemplate.expire(hashKey, BOOK_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /** Redis Hash → BookInfo */
    private BookInfo mapToBookInfo(Map<Object, Object> map) {
        BookInfo book = new BookInfo();
        book.setId((Integer) map.get("id"));
        book.setBookName((String) map.get("bookName"));
        book.setAuthor((String) map.get("author"));
        book.setPublish((String) map.get("publish"));
        book.setPrice((BigDecimal) map.get("price"));
        book.setStatus((Integer) map.get("status"));
        book.setIsBorrowedByMe((Boolean) map.get("isBorrowedByMe"));
        return book;
    }

    /** BookInfo → Map */
    private Map<String, Object> BookInfoToMap(BookInfo book) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", book.getId());
        map.put("bookName", book.getBookName());
        map.put("author", book.getAuthor());
        map.put("publish", book.getPublish());
        map.put("price", book.getPrice());
        map.put("status", book.getStatus());
        map.put("isBorrowedByMe", book.getIsBorrowedByMe());
        return map;
    }

    // 分页获取图书列表，附带当前用户的借阅状态
    public PageResult<BookInfo> getBookListByPage(PageRequest pageRequest, Integer userId) {
        return getBookListByOffset(pageRequest.getOffset(), pageRequest.getPageSize(), userId);
    }

    // 按照偏移量获取图书列表，附带当前用户的借阅状态
    public PageResult<BookInfo> getBookListByOffset(int offset, int limit, Integer userId) {
        // 1. 从Redis获取当前页的图书ID集合
        Set<Object> bookIdSet = redisTemplate.opsForZSet().range(BOOK_INDEX_KEY, offset, offset + limit - 1);
        List<BookInfo> books = new ArrayList<>();
        List<Integer> bookIdList = new ArrayList<>(); // 用于收集ID，方便后续批量操作

        if (bookIdSet != null && !bookIdSet.isEmpty()) {
            // 将Set转为List<Integer>
            for (Object idObj : bookIdSet) {
                Integer bookId = (Integer) idObj;
                bookIdList.add(bookId);
            }

            // 2. 批量从Redis获取图书详情，减少网络IO
            // (存在改进点：pipeline) completely
            List<BookInfo> booksFromCache = new ArrayList<>();
            List<Integer> missIds = new ArrayList<>();

            @SuppressWarnings({ "rawtypes", "unchecked" })
            List<Object> results = redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                public Object execute(RedisOperations operations) throws DataAccessException {
                    for (Integer bookId : bookIdList) {
                        String hashKey = BOOK_INFO_KEY_PREFIX + bookId;
                        operations.opsForHash().entries(hashKey);
                    }
                    return null; // pipeline必须返回null
                }
            });

            // 处理结果
            for (int i = 0; i < bookIdList.size(); i++) {
                Integer bookId = bookIdList.get(i);
                @SuppressWarnings("unchecked")
                Map<Object, Object> map = (Map<Object, Object>) results.get(i);

                if (map == null || map.isEmpty()) {
                    missIds.add(bookId);
                } else {
                    booksFromCache.add(mapToBookInfo(map));
                    logger.info("Cache hit for book ID: " + bookId);
                }
            }

            // 3. 批量查询MySQL，补全缓存未命中的图书详情 (IN查询)
            if (!missIds.isEmpty()) {
                List<BookInfo> booksFromDB = bookInfoMapper.queryBooksByIdList(missIds);
                booksFromCache.addAll(booksFromDB);

                // 异步写回缓存
                CompletableFuture.runAsync(() -> {
                    redisTemplate.executePipelined(new SessionCallback<Object>() {
                        @Override
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        public Object execute(RedisOperations operations) throws DataAccessException {
                            for (BookInfo book : booksFromDB) {
                                String hashKey = BOOK_INFO_KEY_PREFIX + book.getId();
                                operations.opsForHash().putAll(hashKey, BookInfoToMap(book));
                            }
                            return null;
                        }
                    });
                });
            }

            // 4. 不按原始ID列表顺序排序
            books.addAll(booksFromCache);


            // 5. 批量查询当前用户对这些书的借阅状态
            if (userId != null) {
                // 一次查询，获取用户借阅了当前页中的哪些书
                List<Integer> borrowedBookIds = borrowInfoMapper.queryBorrowedBookIdsByUserAndBookList(userId,
                        bookIdList);

                // 遍历当前页的书籍，设置借阅状态
                for (BookInfo book : books) {
                    // 判断集合中是否包含该书ID
                    book.setIsBorrowedByMe(borrowedBookIds.contains(book.getId()));
                }
            }
        }
        int totalCount = bookInfoMapper.countBooks();
        return new PageResult<>(totalCount, books);
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
        if (borrowInfoMapper.insertBorrowInfo(borrowInfo) < 0)
            return false;
        borrowInfo = borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(bookId, userId);
        if (borrowHistoryMapper.insertBorrowInfo(borrowInfo) < 0)
            return false;

        bookInfo.setStatus(BookStatus.FORBIDDEN.getCode());
        saveBookToRedis(bookInfo); // 更新缓存
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
        if (borrowInfoMapper.deleteBorrowInfo(borrowInfo.getBookId(), borrowInfo.getUserId()) < 0)
            return false;
        BookInfo bookInfo = bookInfoMapper.queryBookById(bookId);
        // 保险起见，再次确认图书状态
        bookInfo.setStatus(BookStatus.NORMAL.getCode());
        saveBookToRedis(bookInfo); // 更新缓存
        return true;
    }

    // 获取图书数量
    public int getBookCount() {
        return redisTemplate.opsForZSet().size(BOOK_INDEX_KEY).intValue();
    }
}
