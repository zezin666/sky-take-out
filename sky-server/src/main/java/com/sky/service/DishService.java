package com.sky.service;

import com.sky.dto.DishDTO;
import org.springframework.stereotype.Service;

@Service

public interface DishService {
    //新增菜品以及对应口味类型
    public void saveWithFlavor(DishDTO dishDTO);
}
