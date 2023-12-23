package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;
import org.springframework.stereotype.Service;

import java.util.List;

public interface SetmealService {
    void save(SetmealDTO setmealDTO);

    PageResult page(SetmealPageQueryDTO setmealPageQueryDTO);

    void delete(List<Long> ids);

    SetmealVO getById(Long id);

    void updateWithSetmealDish(SetmealDTO setmealDTO);

    void updateStatus(Integer status, Long id);
}
