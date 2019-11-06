package com.mmall.controller.potal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.mmall.common.Const;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/9/14/ 9:41
 */
@Controller
@RequestMapping("/order/")
public class OrderController {
    private final Logger logger = LoggerFactory.getLogger(OrderController.class);
    @Autowired
    private IOrderService orderService;

    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpServletRequest request, Integer shippingId) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.createOrder(user.getId(), shippingId);
    }
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpServletRequest request, Long orderNo) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.cancel(user.getId(), orderNo);
    }
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpServletRequest request) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.getOrderCheckProduct(user.getId());
    }
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpServletRequest request, Long orderNo) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.getOrderDetail(user.getId(), orderNo);
    }
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpServletRequest request, @RequestParam(value = "pageNum", defaultValue = "1") int pageNum, @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.getOrderList(user.getId(), pageNum, pageSize);
    }



    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(Long orderNo, HttpServletRequest request) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);
        return orderService.pay(user.getId(), orderNo);
    }

    @RequestMapping("alipay_callback.do")
    @ResponseBody
    public Object callback(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>(0b10000);
        Map<String, String[]> requestParams = request.getParameterMap();
        for (Map.Entry<String, String[]> next : requestParams.entrySet()) {
            String key = next.getKey();
            String[] values = next.getValue();
            StringBuilder valueStr = new StringBuilder();
            for (int i = 0; i < values.length; i++) {
                valueStr.append((i == values.length - 1) ? values[i] : values[i] + ",");
            }
            //乱码解决，这段代码在出现乱码时使用
            //valueStr = new String(valueStr.getBytes("ISO-8859-1"), "utf-8");
            params.put(key, valueStr.toString());
        }

        logger.info("支付宝回调：sign:{},Trade_status:{},参数:{}", params.get("sign"), params.get("Trade_status"), params.toString());
        // 验证支付宝的回调正确性，并且避免重复通知。

        Configs.init("zfbinfo.properties");
        try {
            /*
            参数一：params，回调的参数
            参数二：public key，阿里的公钥
            参数三：charset,必须和签名的charset保持一致
            参数四：sign_type，默认是RSA1，但是，现在使用的都是RSA2，所以必传RSA2
             */
            // 官方文档中明确要求过要将sign_type、sign（sign已自动去掉了，见源码）去掉这两个参数。
            params.remove("sign_type");

            boolean checkRes = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(), "utf-8", Configs.getSignType());
            if (!checkRes) {
                return ServerResponse.createBySuccessMsg("非法请求，你再给我乱搞鬼的话，网警了解一下！");
            }

        } catch (AlipayApiException e) {
            logger.error("支付宝回调验证异常。", e);
            return "验签参数是错的，验签不通过！";
        }

/*
商户需要验证该通知数据中的out_trade_no是否为商户系统中创建的订单号，
并判断total_amount是否确实为该订单的实际金额（即商户订单创建时的金额），
同时需要校验通知中的seller_id（或者seller_email) 是否为out_trade_no这笔单据的对应的操作方（有的时候，一个商户可能有多个seller_id/seller_email），
上述有任何一个验证不通过，则表明本次通知是异常通知，务必忽略。
在上述验证通过后商户必须根据支付宝不同类型的业务通知，正确的进行不同的业务处理，并且过滤重复的通知结果数据。
在支付宝的业务通知中，只有交易通知状态为TRADE_SUCCESS或TRADE_FINISHED时，支付宝才会认定为买家付款成功。
*/

        ServerResponse res = orderService.alicallback(params);
        if (res.isSuccess()) {
            // 给支付宝的回调一个响应，返回"success"，支付宝就不会进行重复回调了，本次交易结束。
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.TRADE_STATUS_WAIT_BUYER_PAY;
    }

    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse queryOrderPayStatus(HttpServletRequest request, Long orderNo) {
        User user = (User) request.getAttribute(Const.CURRENT_USER);

        return orderService.selectOrderPayStatus(user.getId(), orderNo);
    }
}
