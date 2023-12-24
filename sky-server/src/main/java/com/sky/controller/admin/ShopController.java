package com.sky.controller.admin;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("adminShopController")
@RequestMapping("/admin/shop")
@Api(tags = "营业状态相关接口")
@Slf4j
public class ShopController {
    public static final String key = "SHOP_STATUS";
    @Autowired
    RedisTemplate redisTemplate;

    @PutMapping("/{status}")
    @ApiOperation("设置营业状态")
    public Result setStatus(@PathVariable Integer status){
        log.info("设置店铺营业状态位：{}",status == 1 ? "营业中" : "打烊");
        redisTemplate.opsForValue().set(key,status);
        return Result.success();
    }

    @GetMapping
    @ApiOperation("查询营业状态")
    public Result<Integer> getStatus(){
        Integer shopStatus = (Integer) redisTemplate.opsForValue().get(key);
        log.info("店铺状态：{}",shopStatus == 1 ? "营业中" : "打烊");
        return Result.success(shopStatus);
    }
}
