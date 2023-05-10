package com.project.vinylata.Service;

import com.project.vinylata.DTO.AddToCartDto;
import com.project.vinylata.DTO.CartDto;
import com.project.vinylata.Model.Cart;
import com.project.vinylata.Model.User;
import jakarta.servlet.http.HttpServletRequest;

public interface ICartService {
    public CartDto listCartItems(Cart cart);

    public void addToCart(AddToCartDto addToCartDto, HttpServletRequest request, User user);
}
