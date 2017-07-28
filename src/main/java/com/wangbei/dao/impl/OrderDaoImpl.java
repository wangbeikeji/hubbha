package com.wangbei.dao.impl;

import com.wangbei.dao.OrderDao;
import com.wangbei.dao.impl.jpa.OrderRepository;
import com.wangbei.entity.Orders;
import com.wangbei.util.enums.OrderStatusEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * @author Created by yuyidi on 2017/7/28.
 * @desc
 */
@Repository
public class OrderDaoImpl implements OrderDao {

	@Autowired
	private OrderRepository orderRepository;

	@Override
	public Orders create(Orders order) {
		return orderRepository.save(order);
	}

	@Override
	public void deleteById(Integer id) {

	}

	@Override
	public Orders update(Orders order) {
		return null;
	}

	@Override
	public Orders retrieveById(Integer id) {
		return null;
	}

	@Override
	public Page<Orders> page(int page, int limit) {
		return null;
	}

	@Override
	public List<Orders> list() {
		return null;
	}

	@Override
	public Integer updateOrderStatusAndModifyTime(String orderNo, String thridOrderNo, OrderStatusEnum orderStatusEnum,
			Date modifyTime) {
		return orderRepository.updateStatusAndTime(orderNo, thridOrderNo, orderStatusEnum, modifyTime);
	}

	@Override
	public Orders getOrderByTradeNo(String tradeNo) {
		return orderRepository.findByTradeNo(tradeNo);
	}
}
