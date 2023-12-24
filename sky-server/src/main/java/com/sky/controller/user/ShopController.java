package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userSHopController")
@RequestMapping("/user/shop")
@Api(tags = "营业状态相关接口")
@Slf4j
public class ShopController {
    public static final String key = "SHOP_STATUS";

    @Autowired
    RedisTemplate redisTemplate;

    @GetMapping
    @ApiOperation("查询营业状态")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(key);
        log.info("店铺状态：{}",shopStatus == 1 ? "营业中" : "打烊");
        return Result.success(shopStatus);
    }
}
