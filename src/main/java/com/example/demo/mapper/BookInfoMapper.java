package com.example.demo.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.demo.model.BookInfo;

@Mapper
public interface BookInfoMapper {
    // 插入操作
    @Insert("insert into book_info (id, book_name, author, donor_id, price, publish, category_ids, tags, description, cover_url) values (#{id}, #{bookName}, #{author}, #{donorId}, #{price}, #{publish}, #{categoryIds}, #{tags}, #{description}, #{coverUrl})")
    Integer insertBook(BookInfo bookInfo);


    // 更新操作
    // 更新书名
    @Update("update book_info set book_name = #{bookName} where id = #{id}")
    Integer updateBookNameById(@Param("id") Integer id, @Param("bookName") String bookName);

    // 更新状态
    @Update("update book_info set status = #{status} where id = #{id}")
    Integer updateBookStatusById(@Param("id") Integer id, @Param("status") Integer status);

    // 更新作者
    @Update("update book_info set author = #{author} where id = #{id}")
    Integer updateAuthorById(@Param("id") Integer id, @Param("author") String author);

    // 更新价格
    @Update("update book_info set price = #{price} where id = #{id}")
    Integer updatePriceById(@Param("id") Integer id, @Param("price") BigDecimal price);

    
    // 更新出版信息
    @Update("update book_info set publish = #{publish} where id = #{id}")
    Integer updatePublishById(@Param("id") Integer id, @Param("publish") String publish);

    // 单独更新 category_ids（方便前端只更新分类的场景）
    @Update("update book_info set category_ids = #{categoryIds} where id = #{id}")
    Integer updateCategoryIdsById(@Param("id") Integer id, @Param("categoryIds") String categoryIds);

     // 单独更新 tags
    @Update("update book_info set tags = #{tags} where id = #{id}")
    Integer updateTagsById(@Param("id") Integer id, @Param("tags") String tags);

    // 单独更新 description
    @Update("update book_info set description = #{description} where id = #{id}")
    Integer updateDescriptionById(@Param("id") Integer id, @Param("description") String description);


    // 全量更新（包含 category_ids, status）
    @Update("update book_info set book_name = #{bookName}, author = #{author}, donor_id = #{donorId}, price = #{price}, publish = #{publish}, category_ids = #{categoryIds}, tags = #{tags}, description = #{description}, status = #{status}, cover_url = #{coverUrl} where id = #{id}")
    Integer updateBook(BookInfo bookInfo);

    // PATCH 风格的选择性更新（只更新非 null 字段）
    @Update({
        "<script>",
        "update book_info",
        "<set>",
        "  <if test='bookName != null'>book_name = #{bookName},</if>",
        "  <if test='author != null'>author = #{author},</if>",
        "  <if test='donorId != null'>donor_id = #{donorId},</if>",
        "  <if test='price != null'>price = #{price},</if>",
        "  <if test='publish != null'>publish = #{publish},</if>",
        "  <if test='categoryIds != null'>category_ids = #{categoryIds},</if>",
        "  <if test='tags != null'>tags = #{tags},</if>",
        "  <if test='description != null'>description = #{description},</if>",
        "  <if test='status != null'>status = #{status},</if>",
        "  <if test='coverUrl != null'>cover_url = #{coverUrl},</if>",
        "</set>",
        "where id = #{id}",
        "</script>"
    })
    Integer updateBookSelective(BookInfo bookInfo);
    
    
    // 删除操作
    @Delete("delete from book_info where id = #{id}")
    Integer deleteBookById(Integer id);
    
    
    // 查询操作
    // 全量查询
    @Select("select * from book_info")
    List<BookInfo> queryBookList();

    // 全量查询书籍ID
    @Select("select id from book_info")
    List<Integer> queryAllBookIds();

    // 分页查询书籍信息
    @Select("select * from book_info order by id desc limit #{offset}, #{limit}")
    List<BookInfo> queryBookListByPage(@Param("offset") int offset, @Param("limit") int limit);

    // 单一查询
    @Select("select * from book_info where id = #{id}")
    BookInfo queryBookById(Integer id);

    // 单独查询封面URL
    @Select("select cover_url from book_info where id = #{id}")
    String queryBookCoverById(Integer id);

    // 单独更新封面URL
    @Update("update book_info set cover_url = #{coverUrl} where id = #{id}")
    Integer updateCoverUrlById(@Param("id") Integer id, @Param("coverUrl") String coverUrl);

    // 查询当前用户(捐赠者)的所有书籍，按更新时间倒序
    @Select("select * from book_info where donor_id = #{donorId} order by update_time desc")
    List<BookInfo> queryBooksByDonor(@Param("donorId") Integer donorId);

    // 获取图书总数
    @Select("select count(*) from book_info")
    Integer countBooks();

    // 根据书籍Id列表查询书籍信息
    @Select({
        "<script>",
        "select * from book_info where id in",
        "<foreach item='item' index='index' collection='list' open='(' separator=',' close=')'>",
        "#{item}",
        "</foreach>",
        "</script>"
    })
    List<BookInfo> queryBooksByIdList(List<Integer> list);

    


}
