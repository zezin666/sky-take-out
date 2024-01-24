package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import com.sky.webSocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Value("${sky.shop.address}")
    private String shopAddress;
    @Value("${sky.baidu.ak}")
    private String ak;

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCarMapper shoppingCarMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private WebSocketServer webSocketServer;

    @Transactional
    @Override//用户下单
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        //地址簿为空
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if(addressBook == null){
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        //检验收货地址是否超出配送范围
        checkOutOfRange(addressBook.getCityName()+addressBook.getDistrictName()+addressBook.getDetail());

        //购物车为空
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(BaseContext.getCurrentId());
        List<ShoppingCart> shoppingCarts = shoppingCarMapper.list(shoppingCart);
        if(shoppingCarts == null){
            throw new ShoppingCartBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,orders);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);
        orders.setStatus(Orders.PENDING_PAYMENT);
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        orders.setConsignee(addressBook.getConsignee());
        orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName() + addressBook.getDistrictName() + addressBook.getDetail());
        orders.setUserId(BaseContext.getCurrentId());
        orders.setTablewareNumber(ordersSubmitDTO.getTablewareNumber());
        orders.setTablewareStatus(ordersSubmitDTO.getTablewareStatus());
        orderMapper.insert(orders);

        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCarts) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart,orderDetail);
            orderDetail.setOrderId(orders.getId());
            orderDetailList.add(orderDetail);
        }

        orderDetailMapper.insertBatch(orderDetailList);
        shoppingCarMapper.deleteByUserId(BaseContext.getCurrentId());

        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderTime(orders.getOrderTime())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .build();
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
//
//        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
//            throw new OrderBusinessException("该订单已支付");
//        }

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("code", "ORDERPAID");

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //为替代微信支付成功后的数据库订单状态更新，多定义一个方法进行修改

        Integer OrderPaidStatus = Orders.PAID; //支付状态，已支付

        Integer OrderStatus = Orders.TO_BE_CONFIRMED;  //订单状态，待接单

        //发现没有将支付时间 check_out属性赋值，所以在这里更新

        LocalDateTime check_out_time = LocalDateTime.now();
        Long orderId = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber()).getId();
        orderMapper.updateStatus(OrderStatus, OrderPaidStatus, check_out_time, orderId);

        //webSocket向浏览器推送消息 这段代码本应写在支付成功回调函数 但本dome项目跳过支付流程
        Map map = new HashMap();
        map.put("type",1);//1表示来单提醒 2表示用户催单
        map.put("orderId",orderId);
        map.put("content","订单号：" + ordersPaymentDTO.getOrderNumber());
        String json = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(json);
        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    @Override
    public PageResult page(int page, int pageSize, Integer status) {

        PageHelper.startPage(page,pageSize);

        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        ordersPageQueryDTO.setStatus(status);

        Page<Orders> ordersPage = orderMapper.page(ordersPageQueryDTO);
        List<OrderVO> list = new ArrayList<>();

        if(ordersPage != null && ordersPage.getTotal() > 0){
            for (Orders orders : ordersPage) {
                Long orderId = orders.getId();
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                orderVO.setOrderDetailList(orderDetails);
                list.add(orderVO);
            }
        }
        return new PageResult(ordersPage.getTotal(),list);
    }

    @Override
    public OrderVO details(Long id) {
        Orders orders = orderMapper.getById(id);
        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(orders.getId());

        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders,orderVO);
        orderVO.setOrderDetailList(detailList);

        return orderVO;
    }

    @Override
    public void cancel(Long id) {
        Orders orders = orderMapper.getById(id);
        if (orders == null){
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        if (orders.getStatus() > 2){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }
        Orders orderUpdate = new Orders();
        orderUpdate.setId(orders.getId());
        //此处应添加微信支付退款代码 但本练习项目未添加微信支付相关代码 故直接操作数据库修改支付状态
        orderUpdate.setPayStatus(Orders.REFUND);
        orderUpdate.setStatus(Orders.CANCELLED);
        orderUpdate.setCancelReason("用户取消");
        orderUpdate.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void repetition(Long id) {
        Long userId = BaseContext.getCurrentId();

        List<OrderDetail> detailList = orderDetailMapper.getByOrderId(id);

        List<ShoppingCart> shoppingCarts = new ArrayList<>();

        for (OrderDetail orderDetail : detailList) {
            ShoppingCart shoppingCart = new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart,"id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCarts.add(shoppingCart);
        }

        shoppingCarMapper.insertBatch(shoppingCarts);
    }

    @Override
    public PageResult page(OrdersPageQueryDTO ordersPageQueryDTO) {
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());
        Page<Orders> ordersPage = orderMapper.page(ordersPageQueryDTO);
        List<OrderVO> orderVOList = getOrderVOList(ordersPage);
        return new PageResult(ordersPage.getTotal(),ordersPage.getResult());
    }

    @Override
    public OrderStatisticsVO statistics() {
        Integer confirmed = orderMapper.statistics(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.statistics(Orders.DELIVERY_IN_PROGRESS);
        Integer toBeConfirmed = orderMapper.statistics(Orders.TO_BE_CONFIRMED);

        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        return orderStatisticsVO;
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        Orders orders = new Orders();
        orders.setId(ordersConfirmDTO.getId());
        orders.setStatus(Orders.CONFIRMED);
        orderMapper.update(orders);
    }

    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        Orders orderDB = orderMapper.getById(ordersRejectionDTO.getId());
        if(orderDB == null || !orderDB.getStatus().equals(Orders.TO_BE_CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        //此处应添加订单支付状态查询及退款代码 本项目无支付功能

        Orders orders = new Orders();
        orders.setId(ordersRejectionDTO.getId());
        orders.setStatus(Orders.CANCELLED);
        orders.setRejectionReason(ordersRejectionDTO.getRejectionReason());
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    @Override
    public void cancel(OrdersCancelDTO ordersCancelDTO) {
        //此处应添加订单支付状态查询及退款代码 本项目无支付功能

        Orders orders = new Orders();
        orders.setId(ordersCancelDTO.getId());
        orders.setCancelReason(ordersCancelDTO.getCancelReason());
        orders.setCancelTime(LocalDateTime.now());
        orders.setStatus(Orders.CANCELLED);
        orderMapper.update(orders);
    }

    @Override
    public void delivery(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null || !orderDB.getStatus().equals(Orders.CONFIRMED)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.DELIVERY_IN_PROGRESS);
        orderMapper.update(orders);
    }

    @Override
    public void complete(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null || !orderDB.getStatus().equals(Orders.DELIVERY_IN_PROGRESS)){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(id);
        orders.setStatus(Orders.COMPLETED);
        orderMapper.update(orders);
    }

    @Override
    public void remind(Long id) {
        Orders orderDB = orderMapper.getById(id);
        if(orderDB == null){
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Map map = new HashMap<>();
        map.put("type",2);
        map.put("orderId",id);
        map.put("content","订单号" + orderDB.getNumber());
        String jsonString = JSON.toJSONString(map);
        webSocketServer.sendToAllClient(jsonString);
    }


    private List<OrderVO> getOrderVOList(Page<Orders> ordersPage){
        List<OrderVO> orderVOList = new ArrayList<>();
        List<Orders> ordersList = ordersPage.getResult();
        if(!CollectionUtils.isEmpty(ordersList)){
            for (Orders orders : ordersList){
                OrderVO orderVO = new OrderVO();
                BeanUtils.copyProperties(orders,orderVO);
                String orderDishes = getOrderDishesStr(orders);
                orderVO.setOrderDishes(orderDishes);
                orderVOList.add(orderVO);
            }
        }
        return orderVOList;
    }

    private String getOrderDishesStr(Orders orders) {
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orders.getId());
        List<String> orderDishList = orderDetails.stream().map(x -> {
            return x.getName() + "*" + x.getNumber() + ";";
        }).collect(Collectors.toList());
        return String.join("",orderDishList);
    }

    private void checkOutOfRange(String address){
        Map<String, String> map = new HashMap<>();
        map.put("address",shopAddress);
        map.put("output","json");
        map.put("ak",ak);

        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3?",map);

        JSONObject jsonObject = JSON.parseObject(shopCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("店铺地址解析失败");
        }


        JSONObject location = jsonObject.getJSONObject("result").getJSONObject("location");
        String lng = location.getString("lng");
        String lat = location.getString("lat");
        String shopLngLat = lat + "," + lng;
        log.info("店铺经纬度：{}",shopLngLat);

        map.put("address",address);
        log.info("{}",map.get("address"));
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3?",map);

        jsonObject = JSON.parseObject(userCoordinate);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("收货地址解析失败");
        }

        location = jsonObject.getJSONObject("result").getJSONObject("location");
        lat = location.getString("lat");
        lng = location.getString("lng");
        //用户收货地址经纬度坐标
        String userLngLat = lat + "," + lng;
        log.info("收货地址经纬度：{}",userLngLat);

        map.put("origin",shopLngLat);
        map.put("destination",userLngLat);
        map.put("steps_info","0");

        String json = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/riding?",map);

        jsonObject = JSON.parseObject(json);
        if(!jsonObject.getString("status").equals("0")){
            throw new OrderBusinessException("配送路线规划失败");
        }

        JSONObject result = jsonObject.getJSONObject("result");
        JSONArray jsonArray = (JSONArray) result.get("routes");
        Integer distance = (Integer) ((JSONObject) jsonArray.get(0)).get("distance");
        if(distance > 5000){
            throw new OrderBusinessException("超出配送范围");
        }
    }
}
