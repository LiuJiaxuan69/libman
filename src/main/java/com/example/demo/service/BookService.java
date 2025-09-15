package com.example.demo.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

import com.example.demo.common.BookStatus;
import com.example.demo.common.PageRequest;
import com.example.demo.common.PageResult;
import com.example.demo.common.TimeBase62UUIDGenerator;
import com.example.demo.mapper.BookCategoryMapper;
import com.example.demo.mapper.BookInfoMapper;
import com.example.demo.mapper.BorrowHistoryMapper;
import com.example.demo.mapper.BorrowInfoMapper;
import com.example.demo.model.BookInfo;
import com.example.demo.model.BorrowInfo;

import jakarta.annotation.PostConstruct;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class BookService {
    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Autowired
    private BorrowInfoMapper borrowInfoMapper;

    @Autowired
    private BorrowHistoryMapper borrowHistoryMapper;

    @Autowired
    private BookCategoryMapper bookCategoryMapper;

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
            // 如果图书包含 categoryIds，异步将该书 ID 写入已存在的分类缓存（category:books:<catId>）
            String categoryIdsJson = bookInfo.getCategoryIds();
            if (categoryIdsJson != null && !categoryIdsJson.isBlank()) {
                List<Integer> cats = new ArrayList<>();
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    cats = mapper.readValue(categoryIdsJson, new TypeReference<List<Integer>>() {});
                } catch (Exception ex) {
                    String trimmed = categoryIdsJson.replaceAll("\\[|\\]", "").trim();
                    if (!trimmed.isEmpty()) {
                        String[] parts = trimmed.split(",");
                        for (String p : parts) {
                            try { cats.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignore) {}
                        }
                    }
                }

                if (!cats.isEmpty()) {
                    final List<Integer> toAdd = cats;
                    CompletableFuture.runAsync(() -> {
                        redisTemplate.executePipelined(new SessionCallback<Object>() {
                            @Override
                            @SuppressWarnings({ "rawtypes", "unchecked" })
                            public Object execute(RedisOperations operations) throws DataAccessException {
                                for (Integer c : toAdd) {
                                    String key = "category:books:" + c;
                                    if (operations.hasKey(key)) {
                                        operations.opsForSet().add(key, bookInfo.getId());
                                    }
                                }
                                return null;
                            }
                        });
                    });
                }
            }
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
        book.setCategoryIds((String) map.get("categoryIds"));
        book.setCategoryNames((String) map.get("categoryNames"));
        book.setTags((String) map.get("tags"));
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
        map.put("categoryIds", book.getCategoryIds());
        map.put("categoryNames", book.getCategoryNames());
        map.put("tags", book.getTags());
        return map;
    }

    // 分页获取图书列表，附带当前用户的借阅状态
    public PageResult<BookInfo> getBookListByPage(PageRequest pageRequest, Integer userId) {
        return getBookListByOffset(pageRequest.getOffset(), pageRequest.getPageSize(), userId);
    }

    /**
     * 批量填充BookInfo列表中的categoryNames字段（只访问一次数据库）
     */
    public void fillCategoryNamesForBooks(List<BookInfo> books) {
        // 1. 收集所有分类ID
        Set<Integer> allCategoryIds = new HashSet<>();
        Map<BookInfo, List<Integer>> bookToCategoryIdList = new HashMap<>();
        for (BookInfo book : books) {
            List<Integer> ids = new ArrayList<>();
            String categoryIdsJson = book.getCategoryIds();
            if (categoryIdsJson != null && !categoryIdsJson.isEmpty()) {
                categoryIdsJson = categoryIdsJson.replaceAll("\\[|\\]", "");
                String[] parts = categoryIdsJson.split(",");
                for (String part : parts) {
                    try {
                        int id = Integer.parseInt(part.trim());
                        ids.add(id);
                        allCategoryIds.add(id);
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
            bookToCategoryIdList.put(book, ids);
        }

        if (allCategoryIds.isEmpty()) {
            // 没有分类ID，全部置空
            for (BookInfo book : books) {
                book.setCategoryNames("");
            }
            return;
        }

        // 2. 一次性查所有分类名
        List<Integer> idList = new ArrayList<>(allCategoryIds);
        // 查询分类实体列表
        var categoryList = bookCategoryMapper.getCategoriesByIds(idList);
        Map<Integer, String> idToName = new HashMap<>();
        categoryList.forEach(c -> idToName.put(c.getId(), c.getCategoryName()));

        // 3. 填充每本书的categoryNames
        for (BookInfo book : books) {
            List<Integer> ids = bookToCategoryIdList.get(book);
            List<String> names = ids.stream().map(idToName::get).filter(n -> n != null).collect(Collectors.toList());
            book.setCategoryNames(String.join(",", names));
        }
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

            // 使用复用方法按 ID 列表获取图书（含 Redis 缓存回源与借阅状态设置）
            List<BookInfo> fetched = fetchBooksByIds(bookIdList, userId);
            books.addAll(fetched);
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

    // 根据图书分类 ID 的 JSON 数组获取分类名称列表
    public List<String> getCategoryNamesByJsonIds(String categoryIdsJson) {
        return bookCategoryMapper.getCategoryNamesByJsonIds(categoryIdsJson);
    }
    // 根据图书分类 ID 的 JSON 数组获取所有同时具有这些分类的图书
    public List<BookInfo> getBooksByCategoryIds(String categoryIdsJson) {
        // 首先将JSON数组字符串转换为List<Integer>
        List<Integer> categoryIds = new ArrayList<>();
        if (categoryIdsJson != null && !categoryIdsJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                categoryIds = mapper.readValue(categoryIdsJson, new TypeReference<List<Integer>>() {
                });
            } catch (Exception ex) {
                logger.warning("Failed to parse categoryIds JSON with Jackson, falling back to manual parse: " + ex.getMessage());
                // fallback: try simple comma separated numbers inside brackets
                String trimmed = categoryIdsJson.replaceAll("\\[|\\]", "").trim();
                if (!trimmed.isEmpty()) {
                    String[] parts = trimmed.split(",");
                    for (String p : parts) {
                        try {
                            categoryIds.add(Integer.parseInt(p.trim()));
                        } catch (NumberFormatException ignore) {
                        }
                    }
                }
            }
        }
        
        // 如果没有分类ID，返回空
        if (categoryIds.isEmpty()) return new ArrayList<>();

        // 1) 批量 pipeline 读取所有 category:books:<id>（使用 Set 存储每个分类的书ID）
        List<String> redisKeys = categoryIds.stream().map(id -> "category:books:" + id).collect(Collectors.toList());
        @SuppressWarnings({ "rawtypes", "unchecked" })
        List<Object> pipelineResults = redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                for (String key : redisKeys) {
                    operations.opsForSet().members(key);
                }
                return null;
            }
        });

        // 2) 解析 pipeline 结果并增量计算交集（拿到每个集合就合并），减少内存占用并能早退出
        List<Integer> missingCatIds = new ArrayList<>();
        Set<Integer> intersection = null;
        for (int i = 0; i < pipelineResults.size(); i++) {
            Object res = pipelineResults.get(i);
            Integer catId = categoryIds.get(i);
            if (res == null) {
                missingCatIds.add(catId);
                continue;
            }
                @SuppressWarnings("unchecked")
                Set<Object> members = (Set<Object>) res;
                if (members == null || members.isEmpty()) {
                missingCatIds.add(catId);
                continue;
            }
            // 转为 Integer 集合（注意可能有占位 -1）；兼容 MyBatis/JDBC 返回 Long 等 Number 类型
            Set<Integer> ids = members.stream().map(o -> {
                if (o instanceof Number) return ((Number) o).intValue();
                try { return Integer.parseInt(String.valueOf(o)); } catch (Exception ex) { return null; }
            }).filter(x -> x != null).collect(Collectors.toSet());
            if (ids.contains(-1)) {
                // 占位表示空集合，交集即空，直接返回
                return new ArrayList<>();
            }
            if (intersection == null) {
                intersection = new HashSet<>(ids);
            } else {
                intersection.retainAll(ids);
            }
            if (intersection.isEmpty()) return new ArrayList<>();
        }

        // 3) 对于缺失的分类，一次性从 DB 批量查询 category->book 关系并增量合并交集
        if (!missingCatIds.isEmpty()) {
            var rows = bookCategoryMapper.getBookIdsByCategoryIds(missingCatIds);
            Map<Integer, Set<Integer>> catToBookIds = new HashMap<>();
            for (var row : rows) {
                Object cObj = row.get("categoryId");
                Object bObj = row.get("bookId");
                Integer cId = null;
                Integer bId = null;
                if (cObj instanceof Number) cId = ((Number) cObj).intValue(); else if (cObj != null) cId = Integer.parseInt(String.valueOf(cObj));
                if (bObj instanceof Number) bId = ((Number) bObj).intValue(); else if (bObj != null) bId = Integer.parseInt(String.valueOf(bObj));
                if (cId == null || bId == null) continue;
                Set<Integer> s = catToBookIds.get(cId);
                if (s == null) {
                    s = new HashSet<>();
                    catToBookIds.put(cId, s);
                }
                s.add(bId);
            }

            // 异步一次性写回 Redis（省掉逐个写入）
            CompletableFuture.runAsync(() -> {
                redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        for (Integer cId : missingCatIds) {
                            String key = "category:books:" + cId;
                            Set<Integer> ids = catToBookIds.getOrDefault(cId, java.util.Collections.emptySet());
                            if (!ids.isEmpty()) {
                                operations.opsForSet().add(key, ids.toArray());
                                operations.expire(key, 60, TimeUnit.MINUTES);
                            } else {
                                operations.opsForSet().add(key, -1);
                                operations.expire(key, 5, TimeUnit.MINUTES);
                            }
                        }
                        return null;
                    }
                });
            });

            // 增量合并从 DB 得到的集合到交集中
            for (Integer cId : missingCatIds) {
                Set<Integer> ids = catToBookIds.getOrDefault(cId, java.util.Collections.emptySet());
                if (ids.isEmpty()) {
                    return new ArrayList<>();
                }
                if (intersection == null) {
                    intersection = new HashSet<>(ids);
                } else {
                    intersection.retainAll(ids);
                }
                if (intersection.isEmpty()) return new ArrayList<>();
            }
        }

        if (intersection == null || intersection.isEmpty()) return new ArrayList<>();
        List<Integer> finalBookIds = new ArrayList<>(intersection);
        return fetchBooksByIds(finalBookIds, null);
    }

    /**
     * 根据 ID 列表批量获取图书：优先从 Redis Hash 中通过 pipeline 获取，缺失则批量回源 MySQL 并异步写回 Redis；
     * 最后根据 userId 批量设置 isBorrowedByMe 字段（若 userId 为 null，则跳过）
     */
    private List<BookInfo> fetchBooksByIds(List<Integer> bookIdList, Integer userId) {
        List<BookInfo> result = new ArrayList<>();
        if (bookIdList == null || bookIdList.isEmpty()) return result;

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
                return null;
            }
        });

        for (int i = 0; i < bookIdList.size(); i++) {
            Integer bookId = bookIdList.get(i);
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) results.get(i);
            if (map == null || map.isEmpty()) {
                missIds.add(bookId);
            } else {
                booksFromCache.add(mapToBookInfo(map));
            }
        }

        if (!missIds.isEmpty()) {
            List<BookInfo> booksFromDB = bookInfoMapper.queryBooksByIdList(missIds);
            // 填充分类名称
            fillCategoryNamesForBooks(booksFromDB);
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

        result.addAll(booksFromCache);

        // 设置借阅状态
        if (userId != null && !result.isEmpty()) {
            List<Integer> ids = result.stream().map(BookInfo::getId).collect(Collectors.toList());
            List<Integer> borrowed = borrowInfoMapper.queryBorrowedBookIdsByUserAndBookList(userId, ids);
            for (BookInfo b : result) {
                b.setIsBorrowedByMe(borrowed.contains(b.getId()));
            }
        }

        return result;
    }
}
