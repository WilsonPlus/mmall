package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Product;
import com.mmall.vo.ProductDetailVo;
import org.apache.ibatis.annotations.Param;

/**
 * @author YaningLiu
 * @date 2018/9/9/ 9:41
 */
public interface IProductService {
    /**
     * 保存或更新商品信息
     *
     * @param product 商品对象
     * @return 返回一个serverresponse
     */
    ServerResponse<String> saveOrUpDateProduct(@Param("product") Product product);

    /**
     * 修改商品的状态：商品状态.1-在售 2-下架 3-删除
     *
     * @param productId 商品的id
     * @param status    需要修改商品的状态
     * @return 返回一个高复用的响应对象
     */
    ServerResponse<String> setProductStatus(Integer productId, Integer status);

    ServerResponse<ProductDetailVo> getDetail(Integer productId);

    ServerResponse<PageInfo> getProducts(Integer pageNum, Integer pageSize);

    ServerResponse searchProduct(String productName, Integer productId, int pageNum, int pageSize);

    ServerResponse<ProductDetailVo> managerGetDetail(Integer productId);

    /**
     * 根据product的categoryID或关键字进行查询
     *
     * @param keyword    关键字
     * @param categoryId 分类id
     * @param pageNum    页数
     * @param pageSize   页面大小
     * @param orderBy    指定排序的字段
     * @return
     */
    ServerResponse<PageInfo> getProductByKeywordCategory(String keyword, Integer categoryId, int pageNum, int pageSize, String orderBy);
}
