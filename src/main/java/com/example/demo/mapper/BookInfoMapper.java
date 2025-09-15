package com.example.demo.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.demo.model.BookInfo;

@Mapper
public interface BookInfoMapper {
    // 插入操作
    @Insert("insert into book_info (id, book_name, author, donor_id, price, publish, category_ids) values (#{id}, #{bookName}, #{author}, #{donorId}, #{price}, #{publish}, #{categoryIds})")
    Integer insertBook(BookInfo bookInfo);

    // 更新操作
    // 全量更新
    @Update("update book_info set book_name = #{bookName}, author = #{author}, donor_id = #{donorId}, price = #{price}, publish = #{publish} where id = #{id}")
    Integer updateBook(BookInfo bookInfo);

    // 更新书名
    @Update("update book_info set book_name = #{bookName} where id = #{id}")
    Integer updateBookNameById(Integer id, String bookName);

    // 更新状态
    @Update("update book_info set status = #{status} where id = #{id}")
    Integer updateBookStatusById(Integer id, Integer status);

    // 更新作者
    @Update("update book_info set author = #{author} where id = #{id}")
    Integer updateAuthorById(Integer id, String author);

    // 更新价格
    @Update("update book_info set price = #{price} where id = #{id}")
    Integer updatePriceById(Integer id, BigDecimal price);

    
    // 更新出版信息
    @Update("update book_info set publish = #{publish} where id = #{id}")
    Integer updatePublishById(Integer id, String publish);
    
    
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
    List<BookInfo> queryBookListByPage(int offset, int limit);

    // 单一查询
    @Select("select * from book_info where id = #{id}")
    BookInfo queryBookById(Integer id);

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
