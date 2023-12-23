package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);

    @AutoFill(OperationType.INSERT)
    void save(Setmeal setmeal);

    Page<SetmealVO> page(SetmealPageQueryDTO setmealPageQueryDTO);

    @Select("select * from sky_take_out.setmeal where id = #{id}")
    Setmeal getById(Long id);

    @Delete("delete from sky_take_out.setmeal where id = #{setmealId}")
    void deleteById(Long setmealId);

    @AutoFill(OperationType.UPDATE)
    void update(Setmeal setmeal);

}
