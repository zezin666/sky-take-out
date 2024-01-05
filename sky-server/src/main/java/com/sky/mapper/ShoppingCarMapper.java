package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ShoppingCarMapper {
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    @Update("update sky_take_out.shopping_cart set number = #{number} where id = #{id}")
    void updateById(ShoppingCart cart);

    @Insert("insert into sky_take_out.shopping_cart (name,user_id,dish_id,setmeal_id,dish_flavor,number,amount,image,create_time)" +
            "values (#{name},#{userId},#{dishId},#{setmealId},#{dishFlavor},#{number},#{amount},#{image},#{createTime})")
    void insert(ShoppingCart shoppingCart);

    @Delete("delete from sky_take_out.shopping_cart where user_id = #{userId}")
    void updateByUserId(Long userId);
}
