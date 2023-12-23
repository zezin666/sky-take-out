package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/setmeal")
@Api(tags = "套餐接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    @PostMapping
    @ApiOperation("新增套餐")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        setmealService.save(setmealDTO);
        return  Result.success();
    }

    @GetMapping("/page")
    @ApiOperation("套餐分页查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        PageResult pageResult = setmealService.page(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    @DeleteMapping
    @ApiOperation("批量删除套餐")
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除套餐");
        setmealService.delete(ids);
        return Result.success();
    }

    @GetMapping("/{id}")
    @ApiOperation("根据id查询套餐")
    public Result<SetmealVO> getById(@PathVariable Long id){
        SetmealVO setmealVO = setmealService.getById(id);
        return Result.success(setmealVO);
    }

    @PutMapping
    @ApiOperation("修改套餐信息")
    public Result update(@RequestBody SetmealDTO setmealDTO){
        setmealService.updateWithSetmealDish(setmealDTO);
        return Result.success();
    }

    @PostMapping("/status/{status}")
    @ApiOperation("修改套餐状态")
    public Result updateStatus(@PathVariable Integer status,Long id){
        setmealService.updateStatus(status,id);
        return Result.success();
    }
}
