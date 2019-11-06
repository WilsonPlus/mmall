package com.mmall.controller.backend;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.service.IOrderService;
import com.mmall.vo.OrderVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author YaningLiu
 * @date 2018/9/17/ 17:02
 */
@Controller
@RequestMapping("/manage/order/")
public class OrderManagerController {
    private final IOrderService orderService;

    @Autowired
    public OrderManagerController(IOrderService orderService) {
        this.orderService = orderService;
    }

    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse<PageInfo> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                         @RequestParam(defaultValue = "10") Integer pageSize) {
        return orderService.managerList(pageNum,pageSize);
    }


    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse<OrderVo> datail(Long orderNo) {
        return orderService.managerDetail(orderNo);
    }

    //这个搜索为了之后进行扩展，演变模糊匹配，支持多条件组合查询，需要添加分页。
    @RequestMapping("search.do")
    @ResponseBody
    public ServerResponse<PageInfo> orderSearch(Long orderNo,
                                               @RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize) {

        return orderService.managerSearch(orderNo,pageNum,pageSize);
    }
    @RequestMapping("send_goods.do")
    @ResponseBody
    public ServerResponse<String> sendGoods(Long orderNo){
        return orderService.managerSendGoods(orderNo);
    }

}
