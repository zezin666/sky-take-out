package com.sky.service;

import com.sky.dto.ShoppingCartDTO;
import com.sky.entity.ShoppingCart;

import java.util.List;

public interface ShoppingCarService {
    void addShoppingCar(ShoppingCartDTO shoppingCartDTO);
    List<ShoppingCart> showCart();

    void clean();
}
