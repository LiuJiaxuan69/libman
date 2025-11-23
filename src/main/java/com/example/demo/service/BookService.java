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
    public boolean addBook(BookInfo bookInfo) {
        // 如果传入的 bookInfo 没有 ID，就生成一个
        if (bookInfo.getId() == null) {
            String generatedId = TimeBase62UUIDGenerator.generate(); // 返回 String
            // 如果 BookInfo 的 id 是 Integer 类型，可以用 hashCode 或者自行修改类型为 String
            bookInfo.setId(generatedId.hashCode());
        }

        // 设置默认封面如果未提供
        if (bookInfo.getCoverUrl() == null || bookInfo.getCoverUrl().isBlank()) {
            bookInfo.setCoverUrl("default.png");
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
                    cats = mapper.readValue(categoryIdsJson, new TypeReference<List<Integer>>() {
                    });
                } catch (Exception ex) {
                    String trimmed = categoryIdsJson.replaceAll("\\[|\\]", "").trim();
                    if (!trimmed.isEmpty()) {
                        String[] parts = trimmed.split(",");
                        for (String p : parts) {
                            try {
                                cats.add(Integer.parseInt(p.trim()));
                            } catch (NumberFormatException ignore) {
                            }
                        }
                    }   
                }
                if (!cats.isEmpty()) {
                    final List<Integer> toAdd = cats;

                    // 先同步检查哪些 key 存在
                    List<Integer> existingKeys = new ArrayList<>();
                    for (Integer c : toAdd) {
                        String key = "category:books:" + c;
                        if (redisTemplate.hasKey(key)) { // 同步检查
                            existingKeys.add(c);
                        }
                    }

                    if (!existingKeys.isEmpty()) {
                        CompletableFuture.runAsync(() -> {
                            redisTemplate.executePipelined(new SessionCallback<Object>() {
                                @Override
                                @SuppressWarnings({ "rawtypes", "unchecked" })
                                public Object execute(RedisOperations operations) throws DataAccessException {
                                    for (Integer c : existingKeys) {
                                        String key = "category:books:" + c;
                                        operations.opsForSet().add(key, bookInfo.getId());
                                    }
                                    return null;
                                }
                            });
                        });
                    }
                }

            }
        }

        return inserted;
    }

    /** 将BookInfo存入Redis Hash */
    private void saveBookToRedis(BookInfo book) {
        String hashKey = BOOK_INFO_KEY_PREFIX + book.getId();
        // 使用统一转换，确保 description / categoryIds / categoryNames / tags 也写入缓存
        Map<String, Object> map = BookInfoToMap(book);
        redisTemplate.opsForHash().putAll(hashKey, map);
        redisTemplate.expire(hashKey, BOOK_EXPIRE_MINUTES, TimeUnit.MINUTES);
    }

    /** 对外暴露：刷新单本书籍缓存（用于封面/部分字段更新后） */
    public void refreshBookCache(Integer bookId) {
        if (bookId == null) return;
        BookInfo fresh = bookInfoMapper.queryBookById(bookId);
        if (fresh == null) return;
        // 填充分类名（如果未缓存）
        fillCategoryNamesForBooks(java.util.List.of(fresh));
        saveBookToRedis(fresh);
    }

    /** 权限判断：是否为该书的捐赠者 */
    public boolean isOwner(Integer userId, Integer bookId) {
        if (userId == null || bookId == null) return false;
        BookInfo b = bookInfoMapper.queryBookById(bookId);
        return b != null && b.getDonorId() != null && b.getDonorId().equals(userId);
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
        book.setCoverUrl((String) map.get("coverUrl"));
        book.setDescription((String) map.get("description"));
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
        map.put("coverUrl", book.getCoverUrl());
        map.put("description", book.getDescription());
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
    public BookStatus borrowBook(Integer userId, Integer bookId) {
        // 检查图书是否存在
        BookInfo bookInfo = bookInfoMapper.queryBookById(bookId);
        if(bookInfo == null) {
            return BookStatus.NOTEXIST; // 图书不存在
        }
        if (bookInfo.getStatus() != BookStatus.NORMAL.getCode()) {
            return BookStatus.getNameByCode(bookInfo.getStatus()); // 图书不可借阅
        }

        // 更新图书状态为借出
        bookInfoMapper.updateBookStatusById(bookId, BookStatus.FORBIDDEN.getCode());

        // 记录借阅信息
        BorrowInfo borrowInfo = new BorrowInfo();
        borrowInfo.setBookId(bookId);
        borrowInfo.setUserId(userId);
        if (borrowInfoMapper.insertBorrowInfo(borrowInfo) < 0)
            return BookStatus.DELETED; // 插入借阅信息失败，回滚并返回可借阅状态
        borrowInfo = borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(bookId, userId);
        if (borrowHistoryMapper.insertBorrowInfo(borrowInfo) < 0)
            return BookStatus.DELETED;

        bookInfo.setStatus(BookStatus.FORBIDDEN.getCode());
        saveBookToRedis(bookInfo); // 更新缓存
        return BookStatus.NORMAL;
    }

    // 归还图书
    @Transactional
    public boolean returnBook(Integer userId, Integer bookId) {
        // 检查借阅记录是否存在
        BorrowInfo borrowInfo = borrowInfoMapper.queryBorrowInfoByUserIdAndBookId(bookId, userId);
        if (borrowInfo == null) {
            return false; // 借阅记录不存在
        }

        // 删除借阅记录（先删除以保证后续更新不会留下孤立记录）
        int deleted = borrowInfoMapper.deleteBorrowInfo(borrowInfo.getBookId(), borrowInfo.getUserId());
        if (deleted < 0) {
            throw new RuntimeException("Failed to delete borrow record");
        }

        // 更新图书状态为可借阅
        bookInfoMapper.updateBookStatusById(bookId, BookStatus.NORMAL.getCode());

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
    // 支持 mode：1=交集，2=并集
    public List<BookInfo> getBooksByCategoryIds(String categoryIdsJson, int modeCode) {
        // 首先将JSON数组字符串转换为List<Integer>
        List<Integer> categoryIds = new ArrayList<>();
        if (categoryIdsJson != null && !categoryIdsJson.isBlank()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                categoryIds = mapper.readValue(categoryIdsJson, new TypeReference<List<Integer>>() {
                });
            } catch (Exception ex) {
                logger.warning("Failed to parse categoryIds JSON with Jackson, falling back to manual parse: "
                        + ex.getMessage());
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
        if (categoryIds.isEmpty())
            return new ArrayList<>();

        com.example.demo.common.CategoryQueryMode mode = com.example.demo.common.CategoryQueryMode.fromCode(modeCode);

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

        // 2) 解析 pipeline 结果并增量计算集合（交集或并集），减少内存占用并能早退出
        List<Integer> missingCatIds = new ArrayList<>();
        Set<Integer> resultSet = null;
        boolean isIntersection = mode == com.example.demo.common.CategoryQueryMode.INTERSECTION;
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
                if (o instanceof Number)
                    return ((Number) o).intValue();
                try {
                    return Integer.parseInt(String.valueOf(o));
                } catch (Exception ex) {
                    return null;
                }
            }).filter(x -> x != null).collect(Collectors.toSet());
            if (isIntersection && ids.contains(-1)) {
                // 占位表示空集合，交集即空，直接返回
                return new ArrayList<>();
            }
            if (resultSet == null) {
                resultSet = new HashSet<>(ids);
            } else {
                if (isIntersection) resultSet.retainAll(ids);
                else resultSet.addAll(ids);
            }
            if (isIntersection && resultSet.isEmpty()) return new ArrayList<>();
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
                if (cObj instanceof Number)
                    cId = ((Number) cObj).intValue();
                else if (cObj != null)
                    cId = Integer.parseInt(String.valueOf(cObj));
                if (bObj instanceof Number)
                    bId = ((Number) bObj).intValue();
                else if (bObj != null)
                    bId = Integer.parseInt(String.valueOf(bObj));
                if (cId == null || bId == null)
                    continue;
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

            // 增量合并从 DB 得到的集合到 resultSet（交集或并集）
            for (Integer cId : missingCatIds) {
                Set<Integer> ids = catToBookIds.getOrDefault(cId, java.util.Collections.emptySet());
                if (ids.isEmpty()) {
                    if (isIntersection) return new ArrayList<>();
                    else continue;
                }
                if (resultSet == null) {
                    resultSet = new HashSet<>(ids);
                } else {
                    if (isIntersection) resultSet.retainAll(ids);
                    else resultSet.addAll(ids);
                }
                if (isIntersection && resultSet.isEmpty()) return new ArrayList<>();
            }
        }

        if (resultSet == null || resultSet.isEmpty()) return new ArrayList<>();
        List<Integer> finalBookIds = new ArrayList<>(resultSet);
        return fetchBooksByIds(finalBookIds, null);
    }

    /**
     * 根据 ID 列表批量获取图书：优先从 Redis Hash 中通过 pipeline 获取，缺失则批量回源 MySQL 并异步写回 Redis；
     * 最后根据 userId 批量设置 isBorrowedByMe 字段（若 userId 为 null，则跳过）
     */
    private List<BookInfo> fetchBooksByIds(List<Integer> bookIdList, Integer userId) {
        List<BookInfo> result = new ArrayList<>();
        if (bookIdList == null || bookIdList.isEmpty())
            return result;

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


    // 更新书籍部分方法
    /**
     * Helper：把 categoryIds JSON 字符串解析为 Integer 列表（宽松解析）
     */
    private List<Integer> parseCategoryIds(String categoryIdsJson) {
        List<Integer> cats = new ArrayList<>();
        if (categoryIdsJson == null || categoryIdsJson.isBlank()) return cats;
        try {
            ObjectMapper mapper = new ObjectMapper();
            cats = mapper.readValue(categoryIdsJson, new TypeReference<List<Integer>>() {});
            return cats;
        } catch (Exception ex) {
            // fallback manual parse
            String trimmed = categoryIdsJson.replaceAll("\\[|\\]", "").trim();
            if (!trimmed.isEmpty()) {
                String[] parts = trimmed.split(",");
                for (String p : parts) {
                    try { cats.add(Integer.parseInt(p.trim())); } catch (NumberFormatException ignore) {}
                }
            }
            return cats;
        }
    }

    /**
     * 全量更新一本书（调用 mapper.updateBook），并同步更新 Redis 缓存与分类集合
     */
    @Transactional
    public boolean updateBookFull(BookInfo bookInfo) {
        if (bookInfo == null || bookInfo.getId() == null) return false;
        Integer id = bookInfo.getId();

        // 读取旧数据以便更新分类集合（和用于最终的缓存写入）
        BookInfo old = bookInfoMapper.queryBookById(id);

        int updated = bookInfoMapper.updateBook(bookInfo);
        if (updated <= 0) return false;

        // 读取最新记录并填充分类名
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        if (fresh == null) return false;
        fillCategoryNamesForBooks(List.of(fresh));

        // 更新 Redis Hash
        saveBookToRedis(fresh);

        // 更新 category:books:<id> 集合（异步）
        List<Integer> oldCats = old == null ? List.of() : parseCategoryIds(old.getCategoryIds());
        List<Integer> newCats = parseCategoryIds(fresh.getCategoryIds());
        final List<Integer> toRemove = oldCats.stream().filter(c -> !newCats.contains(c)).collect(Collectors.toList());
        final List<Integer> toAdd = newCats.stream().filter(c -> !oldCats.contains(c)).collect(Collectors.toList());
        if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        for (Integer c : toRemove) {
                            String key = "category:books:" + c;
                            operations.opsForSet().remove(key, id);
                        }
                        for (Integer c : toAdd) {
                            String key = "category:books:" + c;
                            operations.opsForSet().add(key, id);
                            operations.expire(key, 60, TimeUnit.MINUTES);
                        }
                        return null;
                    }
                });
            });
        }

        return true;
    }

    /**
     * PATCH 风格的选择性更新：只更新非 null 字段（调用 mapper.updateBookSelective）
     */
    @Transactional
    public boolean updateBookSelective(BookInfo bookInfo) {
        if (bookInfo == null || bookInfo.getId() == null) return false;
        Integer id = bookInfo.getId();

        // 读取旧数据以便处理分类变更
        BookInfo old = bookInfoMapper.queryBookById(id);

        int updated = bookInfoMapper.updateBookSelective(bookInfo);
        if (updated <= 0) return false;

        // 读取最新并填充
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        if (fresh == null) return false;
        fillCategoryNamesForBooks(List.of(fresh));

        // 更新 Redis 缓存
        saveBookToRedis(fresh);

        // 若 categoryIds 在此次变更中被更新，则调整分类集合
        String newCatJson = fresh.getCategoryIds();
        String oldCatJson = old == null ? null : old.getCategoryIds();
        if ((oldCatJson == null && newCatJson != null) || (oldCatJson != null && !oldCatJson.equals(newCatJson))) {
            List<Integer> oldCats = old == null ? List.of() : parseCategoryIds(oldCatJson);
            List<Integer> newCats = parseCategoryIds(newCatJson);
            final List<Integer> toRemove = oldCats.stream().filter(c -> !newCats.contains(c)).collect(Collectors.toList());
            final List<Integer> toAdd = newCats.stream().filter(c -> !oldCats.contains(c)).collect(Collectors.toList());
            if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
                CompletableFuture.runAsync(() -> {
                    redisTemplate.executePipelined(new SessionCallback<Object>() {
                        @Override
                        @SuppressWarnings({ "rawtypes", "unchecked" })
                        public Object execute(RedisOperations operations) throws DataAccessException {
                            for (Integer c : toRemove) {
                                String key = "category:books:" + c;
                                operations.opsForSet().remove(key, id);
                            }
                            for (Integer c : toAdd) {
                                String key = "category:books:" + c;
                                operations.opsForSet().add(key, id);
                                operations.expire(key, 60, TimeUnit.MINUTES);
                            }
                            return null;
                        }
                    });
                });
            }
        }

        return true;
    }

    /**
     * 单独更新 tags 字段
     */
    @Transactional
    public boolean updateTags(Integer id, String tags) {
        if (id == null) return false;
        int updated = bookInfoMapper.updateTagsById(id, tags);
        if (updated <= 0) return false;
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        if (fresh != null) {
            fillCategoryNamesForBooks(List.of(fresh));
            saveBookToRedis(fresh);
        }
        return true;
    }

    /**
     * 单独更新 description 字段
     */
    @Transactional
    public boolean updateDescription(Integer id, String description) {
        if (id == null) return false;
        int updated = bookInfoMapper.updateDescriptionById(id, description);
        if (updated <= 0) return false;
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        if (fresh != null) {
            fillCategoryNamesForBooks(List.of(fresh));
            saveBookToRedis(fresh);
        }
        return true;
    }

    /**
     * 单独更新 categoryIds（字符串 JSON），并同步调整 category:books 集合
     */
    @Transactional
    public boolean updateCategoryIds(Integer id, String categoryIdsJson) {
        if (id == null) return false;
        // 读旧值
        BookInfo old = bookInfoMapper.queryBookById(id);
        int updated = bookInfoMapper.updateCategoryIdsById(id, categoryIdsJson);
        if (updated <= 0) return false;
        BookInfo fresh = bookInfoMapper.queryBookById(id);
        if (fresh != null) {
            fillCategoryNamesForBooks(List.of(fresh));
            saveBookToRedis(fresh);
        }
        List<Integer> oldCats = old == null ? List.of() : parseCategoryIds(old.getCategoryIds());
        List<Integer> newCats = parseCategoryIds(categoryIdsJson);
        final List<Integer> toRemove = oldCats.stream().filter(c -> !newCats.contains(c)).collect(Collectors.toList());
        final List<Integer> toAdd = newCats.stream().filter(c -> !oldCats.contains(c)).collect(Collectors.toList());
        if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
            CompletableFuture.runAsync(() -> {
                redisTemplate.executePipelined(new SessionCallback<Object>() {
                    @Override
                    @SuppressWarnings({ "rawtypes", "unchecked" })
                    public Object execute(RedisOperations operations) throws DataAccessException {
                        for (Integer c : toRemove) {
                            String key = "category:books:" + c;
                            operations.opsForSet().remove(key, id);
                        }
                        for (Integer c : toAdd) {
                            String key = "category:books:" + c;
                            operations.opsForSet().add(key, id);
                            operations.expire(key, 60, TimeUnit.MINUTES);
                        }
                        return null;
                    }
                });
            });
        }
        return true;
    }
}
