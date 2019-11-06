package com.mmall.vo;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 购物车中的商品Vo
 *
 * @author YaningLiu
 * @date 2018/9/11/ 11:43
 */
@Getter
@Setter
public class CartProductVo {
    //cartId
    private Integer id;
    //用户Id
    private Integer userId;
    //商品Id
    private Integer productId;
    //购物车中此商品的数量
    private Integer quantity;

    //商品名称
    private String productName;
    //商品标题
    private String productSubtitle;
    //商品的主图
    private String productMainImage;
    //商品的价格
    private BigDecimal productPrice;
    //商品的状态
    private Integer productStatus;
    //购物车中这件商品的总价：quantity * productPrice
    private BigDecimal productTotalPrice;
    //商品的库存
    private Integer productStock;

    //此商品是否勾选
    private Integer productChecked;
    //限制数量的一个返回结果
    private String limitQuantity;
}
