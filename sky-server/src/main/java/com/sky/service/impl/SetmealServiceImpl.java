package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private DishMapper dishMapper;

    @Override
    public void save(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.save(setmeal);
        Long setmealId = setmeal.getId();
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealId);
        });
        setmealDishMapper.insertBatch(setmealDishes);
    }

    @Override
    public PageResult page(SetmealPageQueryDTO setmealPageQueryDTO) {
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> setmealPage = setmealMapper.page(setmealPageQueryDTO);
        return new PageResult(setmealPage.getTotal(),setmealPage.getResult());
    }

    @Override
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getById(id);
            if(Objects.equals(setmeal.getStatus(), StatusConstant.ENABLE)){
                log.info("在售套餐删除失败");
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }
        ids.forEach(setmealId->{
            setmealMapper.deleteById(setmealId);
            setmealDishMapper.deleteBySetmealId(setmealId);
        });
    }

    @Override
    public SetmealVO getById(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal,setmealVO);
        List<SetmealDish> dishes = setmealDishMapper.getBySetmealId(id);
        setmealVO.setSetmealDishes(dishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void updateWithSetmealDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        setmealDishes.forEach(setmealDish -> {
            setmealDish.setSetmealId(setmealDTO.getId());
        });
        setmealDishMapper.insertBatch(setmealDishes);

    }

    @Override
    public void updateStatus(Integer status, Long id) {
        if (Objects.equals(status, StatusConstant.ENABLE)){
            List<Dish> dishes = dishMapper.getBySetmealId(id);
            if(dishes != null && !dishes.isEmpty()){
                dishes.forEach(dish -> {
                    if(Objects.equals(dish.getStatus(), StatusConstant.DISABLE)){
                        throw new SetmealEnableFailedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                    }
                });
            }
        }
        Setmeal setmeal = Setmeal.builder()
                .status(status)
                .id(id)
                .build();
        setmealMapper.update(setmeal);
    }
}
