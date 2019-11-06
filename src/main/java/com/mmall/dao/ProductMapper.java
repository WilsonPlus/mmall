package com.mmall.dao;

import com.mmall.pojo.Product;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(Product record);

    int insertSelective(Product record);

    Product selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(Product record);

    int updateByPrimaryKey(Product record);

    List<Product> selectAllProducts();

    List<Product> selectByNameAndProductId(@Param("productName") String productName, @Param("productId") Integer productId);

    // 根据分类id或者product的名称进行查询，返回一个product集合
    List<Product> selectByNameAndCategoryIds(@Param("keyword") String keyword, @Param("ids") List<Integer> ids);

    //根据
    Integer selectStockByProductId(Integer productId);
}