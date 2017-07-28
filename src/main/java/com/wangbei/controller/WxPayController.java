package com.wangbei.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wangbei.pojo.Response;
import com.wangbei.pojo.TradeWithUserMeritValue;
import com.wangbei.security.AuthUserDetails;
import com.wangbei.security.SecurityAuthService;
import com.wangbei.service.wxpay.WxPayService;
import com.wangbei.util.JacksonUtil;
import com.wangbei.util.enums.TradeTypeEnum;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * 支付 Controller
 * 
 * @author luomengan
 *
 */
@RestController
@RequestMapping("/payment")
@Api(description = "支付接口列表")
public class WxPayController {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private WxPayService wxPayService;
	
	@PostMapping("/wxUnifiedOrder")
	@ApiOperation(value = "微信支付统一下单接口（type：0充值 7放生 8功德）")
	public Response<Map<String, Object>> wxUnifiedOrder(int type, double totalFee) {
		AuthUserDetails authUserDetails = SecurityAuthService.getCurrentUser();
		logger.info(String.format("user(%s) unified order(type:%s, totalFee:%s)",
				authUserDetails.getUserId(), type, totalFee));
		Map<String, Object> result = wxPayService.unifiedOrder(authUserDetails.getUserId(), TradeTypeEnum.getByIndex(type), totalFee);
		logger.info(String.format("unified order result:%s", JacksonUtil.encode(result)));
		return new Response<>(result);
	}

	@PostMapping("/wxOrderQuery")
	@ApiOperation(value = "微信支付查询订单接口")
	public Response<TradeWithUserMeritValue> wxOrderQuery(String tradeNo) {
		return new Response<>(wxPayService.orderQuery(tradeNo));
	}

	@PostMapping("/wxNotify")
	@ApiOperation(value = "微信支付回调通知")
	public String wxNotify(HttpServletRequest request) {
		try {
			byte[] bytes = IOUtils.toByteArray(request.getInputStream());
			String xml = new String(bytes, "UTF-8");
			return wxPayService.receiveNotify(xml);
		} catch (IOException e) {
			throw new RuntimeException("读取微信支付回调请求内容失败!", e);
		}
	}
	
}
