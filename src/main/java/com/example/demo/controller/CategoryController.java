package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.common.Result;
import com.example.demo.mapper.BookCategoryMapper;
import com.example.demo.model.BookCategory;

@RequestMapping("/category")
@RestController
public class CategoryController {

    @Autowired
    private BookCategoryMapper bookCategoryMapper;

    @GetMapping("/list")
    public Result<List<BookCategory>> list() {
        List<BookCategory> cats = bookCategoryMapper.getAllCategories();
        return Result.success(cats);
    }
}
