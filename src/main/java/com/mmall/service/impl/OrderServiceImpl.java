package com.mmall.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayMonitorService;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayMonitorServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.service.impl.AlipayTradeWithHBServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.dao.*;
import com.mmall.pojo.*;
import com.mmall.service.IOrderService;
import com.mmall.util.BigDecimalUtil;
import com.mmall.util.DateTimeUtil;
import com.mmall.util.FtpUtil;
import com.mmall.util.PropertiesUtil;
import com.mmall.vo.OrderItemVo;
import com.mmall.vo.OrderProductVo;
import com.mmall.vo.OrderVo;
import com.mmall.vo.ShippingVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author YaningLiu
 * @date 2018/9/14/ 9:55
 */
@Service
@Slf4j
public class OrderServiceImpl implements IOrderService {

	@Autowired
	private OrderMapper orderMapper;
	@Autowired
	private OrderItemMapper orderItemMapper;
	@Autowired
	private PayInfoMapper payInfoMapper;
	@Autowired
	private CartMapper cartMapper;
	@Autowired
	private ProductMapper productMapper;
	@Autowired
	private ShippingMapper shippingMapper;


	private static Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

	@Override
	public ServerResponse pay(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMsg("该订单未进行创建");
		}
		Map<String, String> resMap = Maps.newHashMap();
		resMap.put("orderNo", String.valueOf(orderNo));


		// (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
		// 需保证商户系统端不能重复，建议通过数据库sequence生成，
		String outTradeNo = order.getOrderNo().toString();

		// (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
		String subject = new StringBuilder().append("happymmall扫码支付，订单号：").append(orderNo).toString();

		// (必填) 订单总金额，单位为元，不能超过1亿元
		// 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
		String totalAmount = order.getPayment().toString();

		// (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
		// 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
		String undiscountableAmount = "0";

		// 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
		// 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
		String sellerId = "";

		// 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
		String body = new StringBuilder().append("订单:").append(orderNo).append("购买商品共").append(order.getPayment()).append("（元）").toString();

		// 商户操作员编号，添加此参数可以为商户操作员做销售统计
		String operatorId = "test_operator_id";

		// (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
		String storeId = "test_store_id";

		// 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
		ExtendParams extendParams = new ExtendParams();
		extendParams.setSysServiceProviderId("2088100200300400500");

		// 支付超时，定义为120分钟
		String timeoutExpress = "120m";

		// 商品明细列表，需填写购买商品详细信息，
		List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

		// 获取订单中的商品信息
		List<OrderItem> orderItemList = orderItemMapper.selectByUserIdAndOrderId(userId, orderNo);
		if (!orderItemList.isEmpty()) {
			for (OrderItem item : orderItemList) {
				// 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
				GoodsDetail goods = GoodsDetail.newInstance(item.getProductId().toString(), item.getProductName(),
						BigDecimalUtil.div(item.getCurrentUnitPrice().doubleValue(), 100D).longValue(), item.getQuantity());
				goodsDetailList.add(goods);
			}
		}

		// 创建扫码支付请求builder，设置请求参数
		AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
				.setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
				.setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
				.setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
				.setTimeoutExpress(timeoutExpress)
				.setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
				.setGoodsDetailList(goodsDetailList);


		/** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
		 *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
		 */
		Configs.init("zfbinfo.properties");

		/** 使用Configs提供的默认参数
		 *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
		 */
		AlipayTradeService tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

		// 支付宝当面付2.0服务（集成了交易保障接口逻辑）
		AlipayTradeService tradeWithHBService = new AlipayTradeWithHBServiceImpl.ClientBuilder().build();

		/** 如果需要在程序中覆盖Configs提供的默认参数, 可以使用ClientBuilder类的setXXX方法修改默认参数 否则使用代码中的默认设置 */
		AlipayMonitorService monitorService = new AlipayMonitorServiceImpl.ClientBuilder()
				.setGatewayUrl("http://mcloudmonitor.com/gateway.do").setCharset("GBK")
				.setFormat("json").build();
		AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
		switch (result.getTradeStatus()) {
			case SUCCESS:
				logger.info("支付宝预下单成功: )");

				AlipayTradePrecreateResponse response = result.getResponse();
				dumpResponse(response);
				// 下单成功后进行生成二维码

				String qrPath = PropertiesUtil.getProperty("qrcode.basepath") + dirGenerateByFileName(response.getOutTradeNo());
				String qrFileName = String.format("qr-%s.png", response.getOutTradeNo());
				try {
					// 根据内容生成位矩阵对象
					BitMatrix bitMatrix = content2BitMatrix(response.getQrCode());
					// 通过bitMatrix对象生成一个BufferedImage
					BufferedImage bufferedImage = toBufferedImage(bitMatrix);
					// 调用方法将这个bufferedImage读取到流中
					InputStream inputStream = bufferedImage2Stream(bufferedImage);
					// 将二维码上传到ftp服务器上
					FtpUtil.uploadFile(qrPath, qrFileName, inputStream);

				} catch (WriterException e) {
					logger.error("create QR code error!", e);
					e.printStackTrace();
				} catch (IOException e) {
					logger.error("上传失败咯！", e);
				}
				logger.info("filePath:" + qrPath + qrFileName);
				String qrURL = PropertiesUtil.getProperty("ftp.server.http.prefix") + qrPath + qrFileName;
				resMap.put("qrUrl", qrURL);

				return ServerResponse.createBySuccess(resMap);

			case FAILED:
				logger.error("支付宝预下单失败!!!");
				return ServerResponse.createByErrorMsg("支付宝预下单失败!!!");

			case UNKNOWN:
				logger.error("系统异常，预下单状态未知!!!");
				return ServerResponse.createByErrorMsg("系统异常，预下单状态未知!!!");

			default:
				logger.error("不支持的交易状态，交易返回异常!!!");
				return ServerResponse.createByErrorMsg("不支持的交易状态，交易返回异常!!!");
		}

	}

	@Override
	public ServerResponse alicallback(Map<String, String> params) {
		// 外部交易号(即本站的订单号)
		Long outTradeNo = Long.parseLong(params.get("out_trade_no"));
		// 内部交易号(支付宝交易号)
		String tradeNo = params.get("trade_no");
		// 支付的状态
		String status = params.get("trade_status");

		Order order = orderMapper.selectByOrderNo(outTradeNo);
		if (order == null) {
			return ServerResponse.createByErrorMsg("非本商城的订单，回调忽略！");
		}
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {

//            ????????为什么可用是大于。

			return ServerResponse.createBySuccessMsg("支付宝重复回调");
		}
		if (Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(status)) {
			// 说明付款成功

			order.setStatus(Const.OrderStatusEnum.PAID.getCode());
			order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
			// 更新数据库中的数据
			orderMapper.updateByPrimaryKeySelective(order);
		}
		PayInfo payInfo = new PayInfo();
		payInfo.setUserId(order.getUserId());
		payInfo.setPayPlatform(Const.PayPlatformEnum.ALIPAY.getCode());
		payInfo.setOrderNo(outTradeNo);
		payInfo.setPlatformNumber(tradeNo);
		payInfo.setPlatformStatus(status);

		payInfoMapper.insert(payInfo);

		return ServerResponse.createBySuccess();
	}

	@Override
	public ServerResponse selectOrderPayStatus(Integer userId, Long orderNo) {
		if (orderNo == null) {
			ResponseCode code = ResponseCode.ILLEGAL_ARGUMENT;
			return ServerResponse.createByErrorCodeMessage(code.getCode(), code.getDesc());
		}
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMsg("该用户没有对应的订单");
		}
		if (order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()) {
			return ServerResponse.createBySuccess(true);
		}
		return ServerResponse.createBySuccess(false);
	}

	@Override
	public ServerResponse createOrder(Integer userId, Integer shippingId) {
		//从购物车中获取选中的数据
		List<Cart> cartList = cartMapper.selectCheckedCartByUserId(userId);

		//计算这个订单的总价
		ServerResponse serverResponse = this.getCartOrderItem(userId, cartList);
		if (!serverResponse.isSuccess()) {
			return serverResponse;
		}
		List<OrderItem> orderItemList = (List<OrderItem>) serverResponse.getData();
		BigDecimal payment = this.getOrderTotalPrice(orderItemList);


		//生成订单
		Order order = this.assembleOrder(userId, shippingId, payment);
		if (order == null) {
			return ServerResponse.createByErrorMsg("生成订单错误");
		}

		for (OrderItem orderItem : orderItemList) {
			orderItem.setOrderNo(order.getOrderNo());
		}
		//mybatis 批量插入
		int rowCount = orderItemMapper.batchInsert(orderItemList);
		if (rowCount < 1) {
			return ServerResponse.createByErrorMsg("生成订单错误");
		}

		//生成成功,减少产品的库存
		this.reduceProductStock(orderItemList);
		//清空一下购物车
		this.cleanCart(cartList);

		OrderVo orderVo = assembleOrderVo(order, orderItemList);
		//返回给前端数据
		return ServerResponse.createBySuccess(orderVo);
	}

	@Override
	public ServerResponse<String> cancel(Integer userId, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(userId, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMsg("该用户不存在此订单！");
		}
		// 使用支付宝的退款接口，进行处理
		if (Const.OrderStatusEnum.PAID.getCode() == order.getStatus()) {
			return ServerResponse.createByErrorMsg("已付款，无法取消订单！");
		}
		Order target = new Order();
		target.setStatus(Const.OrderStatusEnum.CANCELED.getCode());
		target.setId(order.getId());
		int rowCount = orderMapper.updateByPrimaryKeySelective(target);
		if (rowCount > 0) {
			return ServerResponse.createBySuccessMsg("取消订单成功！");
		}
		return ServerResponse.createByError();
	}

	@Override
	public ServerResponse getOrderDetail(Integer id, Long orderNo) {
		Order order = orderMapper.selectByUserIdAndOrderNo(id, orderNo);
		if (order == null) {
			return ServerResponse.createByErrorMsg("未找到该订单！");
		}
		List<OrderItem> orderItemVos = orderItemMapper.selectByUserIdAndOrderId(id, orderNo);
		OrderVo orderVo = this.assembleOrderVo(order, orderItemVos);

		return ServerResponse.createBySuccess(orderVo);
	}

	@Override
	public ServerResponse getOrderList(Integer userId, int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);

		List<Order> orderList = orderMapper.selectByUserId(userId);
		if (orderList.isEmpty()) {
			return ServerResponse.createByErrorMsg("当前没有任何订单");
		}
		List<OrderVo> orderVos = assembleOrderVoList(orderList, userId);
		PageInfo pageInfo = new PageInfo(orderList);
		pageInfo.setList(orderVos);
		return ServerResponse.createBySuccess(pageInfo);
	}

	@Override
	public ServerResponse getOrderCheckProduct(Integer userId) {
		OrderProductVo vo = new OrderProductVo();

		List<Cart> carts = cartMapper.selectCheckedCartByUserId(userId);
		ServerResponse<List<OrderItem>> res = this.getCartOrderItem(userId, carts);
		if (!res.isSuccess()) {
			return res;
		}
		List<OrderItem> orderItemList = res.getData();
		List<OrderItemVo> orderItemVoList = Lists.newArrayList();

		for (OrderItem orderItem : orderItemList) {
			OrderItemVo orderItemVo = this.assembleOrderItemVo(orderItem);
			orderItemVoList.add(orderItemVo);
		}


		vo.setOrderItemVoList(orderItemVoList);
		vo.setProductTotalPrice(this.getOrderTotalPrice(orderItemList));
		vo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

		return ServerResponse.createBySuccess(vo);
	}

	@Override
	public ServerResponse<PageInfo> managerList(Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		List<Order> orderList = orderMapper.selectAllOrder();
		List<OrderVo> orderVoList = assembleOrderVoList(orderList, null);
		PageInfo pageInfo = new PageInfo(orderList);
		pageInfo.setList(orderVoList);
		return ServerResponse.createBySuccess(pageInfo);
	}

	@Override
	public ServerResponse managerDetail(Long orderNo) {
		Order order = null;
		if (null == orderNo || null == (order = orderMapper.selectByOrderNo(orderNo))) {
			return ServerResponse.createByErrorMsg("不存在该订单！");
		}

		List<OrderItem> orderItemList = orderItemMapper.selectByUserIdAndOrderId(order.getUserId(), orderNo);
		OrderVo orderVo = assembleOrderVo(order, orderItemList);
		return ServerResponse.createBySuccess(orderVo);
	}

	@Override
	public ServerResponse<PageInfo> managerSearch(Long orderNo, Integer pageNum, Integer pageSize) {
		PageHelper.startPage(pageNum, pageSize);
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null) {
			List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(orderNo);
			OrderVo orderVo = assembleOrderVo(order, orderItemList);

			PageInfo pageInfo = new PageInfo(Lists.newArrayList(order));
			pageInfo.setList(Lists.newArrayList(orderVo));
			return ServerResponse.createBySuccess(pageInfo);
		}
		return ServerResponse.createByErrorMsg("该订单不存在！");
	}

	@Override
	public ServerResponse<String> managerSendGoods(Long orderNo) {
		Order order = orderMapper.selectByOrderNo(orderNo);
		if (order != null && order.getStatus() == Const.OrderStatusEnum.PAID.getCode()) {
			Order target = new Order();
			target.setId(order.getId());
			target.setStatus(Const.OrderStatusEnum.SHIPPED.getCode());
			target.setSendTime(new Date());
			int rowCount = orderMapper.updateByPrimaryKeySelective(target);
			if (rowCount > 0) {
				return ServerResponse.createBySuccessMsg("发货成功");
			}
			return ServerResponse.createByErrorMsg("发货失败");
		}
		return ServerResponse.createByErrorMsg("订单不存在");
	}

	@Override
	public int closeOrder(int hour) {
		Date closeDateTime = DateUtils.addHours(new Date(),-hour);
		List<Order> orderList = orderMapper.selectOrderStatusByCreateTime(Const.OrderStatusEnum.NO_PAY.getCode(),DateTimeUtil.dateToStr(closeDateTime));

		int rowCount = 0;
		for(Order order : orderList){
			//若这个订单超时未支付，将订单中对应的商品的库存还原。

			//获取订单号中的商品列表
			List<OrderItem> orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
			for(OrderItem orderItem : orderItemList){

				//一定要用主键where条件，防止锁表。同时必须是支持MySQL的InnoDB。
				Integer stock = productMapper.selectStockByProductId(orderItem.getProductId());

				//考虑到已生成的订单里的商品，被删除的情况
				if(stock == null){
					continue;
				}
				Product product = new Product();
				product.setId(orderItem.getProductId());
				product.setStock(stock+orderItem.getQuantity());
				productMapper.updateByPrimaryKeySelective(product);
			}
			rowCount += orderMapper.closeOrderByOrderId(order.getId());
			log.info("关闭订单OrderNo：{}",order.getOrderNo());
		}
		return rowCount;
	}


	private List<OrderVo> assembleOrderVoList(List<Order> orderList, Integer userId) {
		List orderVos = Lists.newArrayList();
		for (Order order : orderList) {
			List<OrderItem> orderItemList = Lists.newArrayList();
			// 后台的管理员是不需要传userId的
			if (userId != null) {
				orderItemList = orderItemMapper.selectByUserIdAndOrderId(userId, order.getOrderNo());
			} else {
				orderItemList = orderItemMapper.selectByOrderNo(order.getOrderNo());
			}
			orderVos.add(this.assembleOrderVo(order, orderItemList));
		}
		return orderVos;
	}

	private OrderVo assembleOrderVo(Order order, List<OrderItem> orderItemList) {

		OrderVo orderVo = new OrderVo();
		orderVo.setOrderNo(order.getOrderNo());
		orderVo.setPayment(order.getPayment());
		orderVo.setPaymentType(order.getPaymentType());
		orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.codeOf(order.getPaymentType()).getValue());

		orderVo.setPostage(order.getPostage());
		orderVo.setStatus(order.getStatus());
		orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(order.getStatus()).getValue());

		orderVo.setShippingId(order.getShippingId());
		Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
		if (shipping != null) {
			orderVo.setReceiverName(shipping.getReceiverName());
			orderVo.setShippingVo(assembleShippingVo(shipping));
		}

		orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
		orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
		orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
		orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
		orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));


		orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));


		List<OrderItemVo> orderItemVoList = Lists.newArrayList();

		for (OrderItem orderItem : orderItemList) {
			OrderItemVo orderItemVo = assembleOrderItemVo(orderItem);
			orderItemVoList.add(orderItemVo);
		}
		orderVo.setOrderItemVoList(orderItemVoList);
		return orderVo;
	}

	private OrderItemVo assembleOrderItemVo(OrderItem orderItem) {
		OrderItemVo vo = new OrderItemVo();
		vo.setOrderNo(orderItem.getOrderNo());
		vo.setProductId(orderItem.getProductId());
		vo.setProductName(orderItem.getProductName());
		vo.setProductImage(orderItem.getProductImage());
		vo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
		vo.setQuantity(orderItem.getQuantity());
		vo.setTotalPrice(orderItem.getTotalPrice());
		vo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));

		return vo;
	}

	private ShippingVo assembleShippingVo(Shipping shipping) {
		ShippingVo shippingVo = new ShippingVo();
		shippingVo.setReceiverName(shipping.getReceiverName());
		shippingVo.setReceiverPhone(shipping.getReceiverPhone());
		shippingVo.setReceiverMobile(shipping.getReceiverMobile());
		shippingVo.setReceiverProvince(shipping.getReceiverProvince());
		shippingVo.setReceiverCity(shipping.getReceiverCity());
		shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
		shippingVo.setReceiverAddress(shipping.getReceiverAddress());
		shippingVo.setReceiverZip(shipping.getReceiverZip());

		return shippingVo;
	}

	private void cleanCart(List<Cart> cartList) {
		for (Cart cartItem : cartList) {
			cartMapper.deleteByPrimaryKey(cartItem.getId());
		}
	}

	private void reduceProductStock(List<OrderItem> orderItemList) {
		for (OrderItem orderItem : orderItemList) {
			Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
			product.setStock(product.getStock() - orderItem.getQuantity());
			productMapper.updateByPrimaryKeySelective(product);
		}
	}

	private Order assembleOrder(Integer userId, Integer shippingId, BigDecimal payment) {
		Order order = new Order();
		order.setUserId(userId);
		order.setShippingId(shippingId);

		long orderNo = this.generateOrderNo();
		order.setOrderNo(orderNo);
		order.setStatus(Const.OrderStatusEnum.NO_PAY.getCode());
		order.setPostage(0);
		order.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
		order.setPayment(payment);

		int rowCount = orderMapper.insert(order);
		if (rowCount > 0) {
			return order;
		}
		return null;
	}

	private long generateOrderNo() {
		long currentTime = System.currentTimeMillis();
		return currentTime + new Random().nextInt(100);
	}

	private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList) {
		BigDecimal sum = new BigDecimal("0");
		for (OrderItem orderItem : orderItemList) {
			sum = sum.add(orderItem.getTotalPrice());
		}
		return sum;
	}

	private ServerResponse<List<OrderItem>> getCartOrderItem(Integer userId, List<Cart> cartList) {
		List<OrderItem> orderItemList = Lists.newArrayList();
		if (CollectionUtils.isEmpty(cartList)) {
			return ServerResponse.createByErrorMsg("您当前购物车中并未选中任何商品！");
		}
		for (Cart cartItem : cartList) {
			OrderItem orderItem = new OrderItem();
			Product product = productMapper.selectByPrimaryKey(cartItem.getProductId());
			if (Const.ProductStatusEnum.ON_SALE.getCode() != product.getStatus()) {
				return ServerResponse.createByErrorMsg("产品" + product.getName() + "非在线售卖状态");
			}
			if (cartItem.getQuantity() > product.getStock()) {
				return ServerResponse.createByErrorMsg("产品" + product.getName() + "库存不足");
			}

			orderItem.setUserId(userId);
			orderItem.setProductId(product.getId());
			orderItem.setProductName(product.getName());
			orderItem.setProductImage(product.getMainImage());
			orderItem.setCurrentUnitPrice(product.getPrice());
			orderItem.setQuantity(cartItem.getQuantity());
			orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(), cartItem.getQuantity()));
			orderItemList.add(orderItem);
		}
		return ServerResponse.createBySuccess(orderItemList);
	}


	// 简单打印应答
	private void dumpResponse(AlipayResponse response) {
		if (response != null) {
			logger.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
			if (StringUtils.isNotEmpty(response.getSubCode())) {
				logger.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
						response.getSubMsg()));
			}
			logger.info("body:" + response.getBody());
		}
	}

	// 根据内容生成Bit矩阵
	private BitMatrix content2BitMatrix(String content) throws WriterException {
		int width = 256;
		int height = 256;
		final String FORMARTNAME = "jpg";

		Map<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
		hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
		hints.put(EncodeHintType.CHARACTER_SET, "UTF8");
		BitMatrix bitMatrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
		return bitMatrix;
	}

	// 通过bit矩阵生成一个BufferedImage
	private static BufferedImage toBufferedImage(BitMatrix matrix) {
		final int BLACK = 0xFF000000;
		final int WHITE = 0xFFFFFFFF;

		int width = matrix.getWidth();
		int height = matrix.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, matrix.get(x, y) ? BLACK : WHITE);
			}
		}
		return image;
	}

	// 将根据订单号来生成对应的文件夹，使用hashcode进行目录打散
	private String dirGenerateByFileName(String oldName) {
		if (org.apache.commons.lang3.StringUtils.isBlank(oldName)) {
			return "default/";
		}
		//获取文件名的哈希值，将之转换成16进制。
		int hCode = oldName.hashCode();
		String hex = Integer.toHexString(hCode);

		//将保存的文件路径和截取的字符生成文件目录返回。
		return new String(hex.charAt(0) + "/" + hex.charAt(1) + "/");
	}

	// 将BufferedImage读取到流中。
	private InputStream bufferedImage2Stream(BufferedImage bufferedImage) throws IOException {
		final String FORMARTNAME = "jpg";
		// 下面都是将这个bufferedImage读取到流中
		ByteArrayOutputStream bs = new ByteArrayOutputStream();
		ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(bs);
		// 使用ImageIO对象将bufferedImage读取到流中
		ImageIO.write(bufferedImage, FORMARTNAME, imageOutputStream);
		InputStream inputStream = new ByteArrayInputStream(bs.toByteArray());
		return inputStream;
	}
}