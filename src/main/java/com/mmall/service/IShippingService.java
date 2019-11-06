package com.mmall.service;

import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.Shipping; /**
 * @author YaningLiu
 * @date 2018/9/12/ 20:45
 */
public interface IShippingService {
    ServerResponse add(Integer userId, Shipping shipping);

    //删除一个shipping，加入userId是为了防止横向越权。根据shippingId来删除，缺陷：若这个用户登录后，传递的shipping并不属于自己的话，同样会被删除。
    ServerResponse<PageInfo> list(Integer userId, int pageNum, int pageSize);

    ServerResponse del(Integer userId, Integer shippingId);

    ServerResponse update(Integer userId, Shipping shipping);

    ServerResponse<Shipping> select(Integer userId, Integer shippingId);
}
