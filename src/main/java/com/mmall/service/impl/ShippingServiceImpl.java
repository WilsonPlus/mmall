package com.mmall.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.mmall.common.ServerResponse;
import com.mmall.dao.ShippingMapper;
import com.mmall.pojo.Shipping;
import com.mmall.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author YaningLiu
 * @date 2018/9/12/ 20:45
 */
@Service
public class ShippingServiceImpl implements IShippingService {
    @Autowired
    private ShippingMapper shippingMapper;

    @Override
    public ServerResponse add(Integer userId, Shipping shipping) {
        //数据都没进行校验
        shipping.setUserId(userId);
        Integer rowCount = shippingMapper.insert(shipping);
        if (rowCount > 0) {
            Map<String, Integer> data = new HashMap<String, Integer>(0x10);
            data.put("shippingId", shipping.getId());
            return ServerResponse.createBySuccess("新建地址成功", data);
        }
        return ServerResponse.createBySuccessMsg("新建地址失败");
    }

    @Override
    public ServerResponse del(Integer userId, Integer shippingId) {
        int rowCount = shippingMapper.deleteByShippingIdAndUserId(shippingId, userId);
        if (rowCount > 0) {
            return ServerResponse.createBySuccessMsg("删除地址成功");
        }
        return ServerResponse.createBySuccessMsg("删除地址失败");
    }

    @Override
    public ServerResponse update(Integer userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if (rowCount > 0) {
            return ServerResponse.createBySuccessMsg("更新地址成功");
        }
        return ServerResponse.createBySuccessMsg("更新地址失败");
    }

    @Override
    public ServerResponse<Shipping> select(Integer userId, Integer shippingId) {
        Shipping shipping = shippingMapper.selectByShippingIdAndUserId(shippingId, userId);
        if (shipping == null) {
            return ServerResponse.createByErrorMsg("当前地址不可用");
        }
        return ServerResponse.createBySuccess(shipping);
    }

    @Override
    public ServerResponse<PageInfo> list(Integer id, int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shipping> shippings = shippingMapper.selectByUserId(id);
        PageInfo pageInfo = new PageInfo(shippings);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
