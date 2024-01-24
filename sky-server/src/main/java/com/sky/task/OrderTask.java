package com.sky.task;

import com.sky.entity.Orders;
import com.sky.mapper.OrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class OrderTask {
    @Autowired
    private OrderMapper orderMapper;
    @Scheduled(cron = "00 * * * * ?")
    public void timeoutOrderCheck(){
        log.info("定时处理超时订单 {}", LocalDateTime.now());

        List<Orders> timeoutOrders = orderMapper.timeoutCheck(Orders.PENDING_PAYMENT, LocalDateTime.now().plusMinutes(-15));

        if(timeoutOrders != null && !timeoutOrders.isEmpty()){
            for (Orders order : timeoutOrders) {
                order.setStatus(Orders.CANCELLED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }

    @Scheduled(cron = "0 0 1 * * ?")
    public void deliveryOrderCheck(){
        log.info("定时处理处于派送中订单 {}",LocalDateTime.now());
        List<Orders> deliveryOrders = orderMapper.timeoutCheck(Orders.DELIVERY_IN_PROGRESS,LocalDateTime.now().plusMinutes(-60));
        if(deliveryOrders != null && !deliveryOrders.isEmpty()){
            for (Orders order : deliveryOrders) {
                order.setStatus(Orders.COMPLETED);
                order.setCancelReason("订单超时，自动取消");
                order.setCancelTime(LocalDateTime.now());
                orderMapper.update(order);
            }
        }
    }
}
