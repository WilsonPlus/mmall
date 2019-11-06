package com.mmall.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.CartMapper;
import com.mmall.dao.ProductMapper;
import com.mmall.pojo.Cart;
import com.mmall.pojo.Product;
import com.mmall.service.ICartService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.CartProductVo;
import com.mmall.vo.CartVo;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author YaningLiu
 * @date 2018/9/11/ 11:05
 */
@Service
public class CartServiceImpl implements ICartService {
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;

    @Override
    public ServerResponse<CartVo> list(Integer userId) {
        CartVo res = getCartVoLimit(userId);
        return ServerResponse.createBySuccess(res);
    }

    @Override
    public ServerResponse<CartVo> selectOrUnSelect(Integer userId, Integer productId, Integer checked) {
        cartMapper.checkedProduct(userId, productId, checked);
        return list(userId);
    }

    @Override
    public ServerResponse<Integer> selectCartProductCount(Integer userId) {
        if(userId == null) {
            return ServerResponse.createBySuccess(1);
        }
        return ServerResponse.createBySuccess(cartMapper.selectCartProductCount(userId));
    }

    @Override
    public ServerResponse<CartVo> add(Integer userId, Integer productId, Integer count) {
        if (productId == null) {
            ResponseCode code = ResponseCode.ILLEGAL_ARGUMENT;
            return ServerResponse.createByErrorCodeMessage(code.getCode(), code.getDesc());
        }
        //先看购物车是否存在，存在将购物车进行更新，不存在将购物车插入到数据库中。
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart == null) {
            cart = new Cart();
            cart.setUserId(userId);
            cart.setProductId(productId);
            cart.setQuantity(count);
            cart.setChecked(Const.Cart.CHECKED);
            cartMapper.insert(cart);
        } else {
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            //更新商品数量
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return list(userId);
    }

    @Override
    public ServerResponse<CartVo> update(Integer userId, Integer productId, Integer count) {
        if (userId == null || productId == null) {
            ResponseCode code = ResponseCode.ILLEGAL_ARGUMENT;
            return ServerResponse.createByErrorCodeMessage(code.getCode(), code.getDesc());
        }

        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if (cart != null) {
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKeySelective(cart);
        return list(userId);
    }

    @Override
    public ServerResponse<CartVo> delCartProduct(Integer userId, String productIds) {
        if(StringUtils.isEmpty(productIds)||StringUtils.isBlank(productIds)){
            ResponseCode code = ResponseCode.ILLEGAL_ARGUMENT;
            return ServerResponse.createByErrorCodeMessage(code.getCode(), code.getDesc());
        }
        List<String> productIdArr = Splitter.on(",").splitToList(productIds);

        int rowCount = cartMapper.deleteByUserIdAndProductIds(userId, productIdArr);
        return list(userId);
    }

    //*******************************************************************************
    private CartVo getCartVoLimit(Integer userId) {
        //整个购物车的vo对象，存放着所有的单个商品的vo
        CartVo cartVo = new CartVo();
        List<Cart> carts = cartMapper.selectByUserId(userId);
        //存放购物车中所有的商品
        List<CartProductVo> cartProductVoList = Lists.newArrayList();
        //购物车中的所勾选的商品总价
        BigDecimal cartTotalPrice = new BigDecimal("0");

        if (CollectionUtils.isNotEmpty(carts)) {
            for (Cart cart : carts) {
                //单个商品的vo
                CartProductVo cartProductVo = new CartProductVo();

                cartProductVo.setId(cart.getId());
                cartProductVo.setUserId(cart.getUserId());
                cartProductVo.setProductId(cart.getProductId());
                //cartProductVo.setQuantity(cart.getQuantity());需要先经过判断库存，然后进行一个赋值，

                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                //若这个商品存在，进行cartProductVo的信息补全
                if (product != null) {
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStatus(product.getStatus());
                    cartProductVo.setProductStock(product.getStock());

                    // 判断库存
                    if (product.getStock() >= cart.getQuantity()) {
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                        cartProductVo.setQuantity(cart.getQuantity());
                    } else {
                        //数据库中的商品库存数量小于将要购买的数量时，将返回限制有效数量。
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        cartProductVo.setQuantity(product.getStock());
                        //购物车中更新有效库存
                        Cart cartForQuantity = new Cart();
                        cartForQuantity.setId(cart.getId());
                        cartForQuantity.setQuantity(product.getStock());
                        cartMapper.updateByPrimaryKeySelective(cartForQuantity);
                    }

                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice(), cart.getQuantity()));
                    cartProductVo.setProductChecked(cart.getChecked());
                }
                if (cart.getChecked() == Const.Cart.CHECKED) {
                    //如果已经勾选,增加到整个的购物车总价中
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(), cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);
            }
        }
        //选中的商品的总金额
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        return cartVo;
    }

    private boolean getAllCheckedStatus(Integer userId) {
        if (userId == null) {
            return false;
        }
        return cartMapper.selectIsNotCheckAllByUserId(userId) == 0;

    }
}
