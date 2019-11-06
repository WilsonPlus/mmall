package com.mmall.vo;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author YaningLiu
 * @date 2018/9/6/ 21:40
 */
@Getter
@Setter
public class CartVo {
    //所有的商品集合
    private List<CartProductVo> cartProductVoList;
    //购物车中商品的总金额
    private BigDecimal cartTotalPrice;
    //是否已经都勾选
    private Boolean allChecked;
    //图片的服务器地址
    private String imageHost;
}
