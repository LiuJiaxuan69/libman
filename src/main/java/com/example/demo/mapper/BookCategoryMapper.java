package com.example.demo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import com.example.demo.model.BookCategory;

// 注意：这里需使用 MyBatis 的 @Param，而不是 lettuce 的同名注解
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookCategoryMapper {
    // 根据分类 ID 获取分类名称
        @Select("""
                SELECT c.category_name
                FROM category c
                JOIN JSON_TABLE(
                    #{categorIds},
                    '$[*]' COLUMNS(id INT PATH '$')
                ) jt ON c.id = jt.id
                """)
        List<String> getCategoryNamesByJsonIds(@Param("categorIds") String categorIds);

    // 查询所有分类
    @Select("SELECT * FROM category")
    List<BookCategory> getAllCategories();

    // 根据分类 ID 列表查询分类（不再用 FIELD 排序，交由业务映射）
    @Select({
        "<script>",
        "SELECT id, category_name AS categoryName FROM category WHERE id IN",
        "<foreach item='item' index='index' collection='ids' open='(' separator=',' close=')'>",
        "#{item}",
        "</foreach>",
        "</script>"
    })
    List<BookCategory> getCategoriesByIds(@Param("ids") List<Integer> ids);

    // 根据特定分类 ID 查询所有符合条件的书籍 ID
    // 使用 JSON_CONTAINS 判断 book_info.category_ids JSON 数组中是否包含该分类 ID，
    // 并通过 category 表验证分类存在（等价于 join）
        @Select("SELECT bi.id FROM book_info bi WHERE JSON_CONTAINS(bi.category_ids, JSON_ARRAY(#{categoryId}))")
        List<Integer> getBookIdsByCategoryId(@Param("categoryId") Integer categoryId);

            // 根据多个分类 ID 批量返回 categoryId - bookId 映射（使用 JSON_TABLE 展开 book_info.category_ids）
            @Select({
                "<script>",
                "SELECT CAST(jt.id AS SIGNED) AS categoryId, bi.id AS bookId",
                "FROM book_info bi",
                "JOIN JSON_TABLE(bi.category_ids, '$[*]' COLUMNS(id INT PATH '$')) jt",
                "WHERE jt.id IN",
                "<foreach item='item' collection='ids' open='(' separator=',' close=')'>",
                "#{item}",
                "</foreach>",
                "</script>"
            })
            List<java.util.Map<String, Object>> getBookIdsByCategoryIds(@Param("ids") List<Integer> ids);
}
