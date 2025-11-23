package com.example.demo.ai;

import com.example.demo.mapper.BookInfoMapper;
import com.example.demo.mapper.BookCategoryMapper;
import com.example.demo.mapper.UserInfoMapper;
import com.example.demo.model.BookInfo;
import com.example.demo.model.BookCategory;
import com.example.demo.model.UserInfo;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MapperTools {
    private final BookInfoMapper bookInfoMapper;
    private final BookCategoryMapper categoryMapper;
    private final UserInfoMapper userInfoMapper;

    public MapperTools(BookInfoMapper bookInfoMapper, BookCategoryMapper categoryMapper, UserInfoMapper userInfoMapper) {
        this.bookInfoMapper = bookInfoMapper;
        this.categoryMapper = categoryMapper;
        this.userInfoMapper = userInfoMapper;
    }

    // 简单的类别同义 & 回退映射，可扩展
    private static final Map<String, List<String>> CATEGORY_FALLBACK = Map.of(
            "文学", List.of("艺术", "文化", "历史"),
            "小说", List.of("文学", "艺术"),
            "诗歌", List.of("文学", "艺术"),
            "人工智能", List.of("计算机", "机器学习", "大数据"),
            "网络", List.of("计算机", "通信"),
            "数据库", List.of("计算机", "数据"));

    @Tool("分页列出书籍，按 id 降序。参数 offset 为起始偏移，limit 为数量。")
    public List<BookInfo> listBooksByOffset(int offset, int limit) {
        return bookInfoMapper.queryBookListByPage(offset, limit);
    }

    @Tool("获取图书总数")
    public int countBooks() {
        Integer c = bookInfoMapper.countBooks();
        return c == null ? 0 : c;
    }

    @Tool("根据图书ID获取书籍详细")
    public BookInfo getBookById(int id) {
        return bookInfoMapper.queryBookById(id);
    }

    @Tool("根据图书ID列表批量获取书籍")
    public List<BookInfo> getBooksByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return java.util.Collections.emptyList();
        return bookInfoMapper.queryBooksByIdList(ids);
    }

    @Tool("列出所有分类")
    public List<BookCategory> listCategories() {
        return categoryMapper.getAllCategories();
    }

    @Tool("按分类名称精确或模糊查找书籍，如果该分类不存在则尝试回退同义分类。参数 name 为分类中文名称，limit 为最大返回数量。")
    public List<BookInfo> searchBooksByCategoryWithFallback(String name, int limit) {
        if (name == null || name.isBlank()) return java.util.Collections.emptyList();
        List<BookCategory> all = categoryMapper.getAllCategories();
        // 先尝试精确与包含匹配
        List<Integer> matchedIds = all.stream()
                .filter(c -> c.getCategoryName() != null && (c.getCategoryName().equalsIgnoreCase(name) || c.getCategoryName().contains(name)))
                .map(BookCategory::getId)
                .collect(Collectors.toList());
        if (matchedIds.isEmpty()) {
            // 回退映射
            List<String> fallback = CATEGORY_FALLBACK.getOrDefault(name, List.of());
            if (!fallback.isEmpty()) {
                matchedIds = all.stream()
                        .filter(c -> c.getCategoryName() != null && fallback.stream().anyMatch(f -> c.getCategoryName().contains(f)))
                        .map(BookCategory::getId)
                        .collect(Collectors.toList());
            }
        }
        if (matchedIds.isEmpty()) return java.util.Collections.emptyList();
        // 简单分页抓取较大数量（假设数据量不巨大，可调整抓取深度）
        List<Integer> finalMatchedIds = matchedIds; // 使其 effectively final 供 lambda 使用
        List<BookInfo> batch = bookInfoMapper.queryBookListByPage(0, Math.max(limit, 200));
        List<BookInfo> filtered = batch.stream()
            .filter(b -> containsAnyCategory(b.getCategoryIds(), finalMatchedIds))
                .limit(limit)
                .collect(Collectors.toList());
        return filtered;
    }

    private boolean containsAnyCategory(String categoryIdsJson, List<Integer> targetIds) {
        if (categoryIdsJson == null || categoryIdsJson.isBlank() || targetIds == null || targetIds.isEmpty()) return false;
        String trimmed = categoryIdsJson.trim();
        // 去掉可能的方括号与引号
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) return false;
        String[] parts = trimmed.split(",");
        for (String p : parts) {
            String token = p.replaceAll("[^0-9]", "").trim();
            if (token.isEmpty()) continue;
            try {
                int id = Integer.parseInt(token);
                if (targetIds.contains(id)) return true;
            } catch (NumberFormatException ignored) {}
        }
        return false;
    }

    @Tool("根据用户名查找用户（用于查询捐赠者信息等）")
    public UserInfo getUserByName(String userName) {
        return userInfoMapper.queryUserByName(userName);
    }
}
