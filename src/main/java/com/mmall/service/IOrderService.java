package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.vo.OrderVo;

import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/9/14/ 9:55
 */
public interface IOrderService {
    ServerResponse pay(Integer id, Long orderNo);

    ServerResponse alicallback(Map<String, String> params);

    ServerResponse selectOrderPayStatus(Integer userId, Long orderNo);

    ServerResponse createOrder(Integer id, Integer shippingId);

    ServerResponse cancel(Integer id, Long orderNo);

    ServerResponse getOrderDetail(Integer id, Long orderNo);

    /**
     * 当用户登录后，查看个人的所有订单
     *
     * @param id
     * @param pageNum
     * @param pageSize
     * @return
     */
    ServerResponse getOrderList(Integer id, int pageNum, int pageSize);

    /**
     * 当从购物车中选择商品跳转到结算页面的时候，将选择的商品信息进行显示
     *
     * @param id 用户的id
     * @return 商品的包装对象
     */
    ServerResponse getOrderCheckProduct(Integer id);


//backend

    ServerResponse<PageInfo> managerList(Integer pageNum, Integer pageSize);

    ServerResponse<OrderVo> managerDetail(Long orderNo);

    ServerResponse<PageInfo> managerSearch(Long orderNo, Integer pageNum, Integer pageSize);

    ServerResponse<String> managerSendGoods(Long orderNo);

    //hour个小时之后关闭订单
    int closeOrder(int hour);
}
