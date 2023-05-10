package com.project.vinylata.Service;

import com.project.vinylata.DTO.AddToCartDto;
import com.project.vinylata.DTO.CartDto;
import com.project.vinylata.DTO.CartItemDto;
import com.project.vinylata.Exception.ProductNotExistException;
import com.project.vinylata.Model.Cart;
import com.project.vinylata.Model.CartItem;
import com.project.vinylata.Model.Product;
import com.project.vinylata.Model.User;
import com.project.vinylata.Repository.CartItemRepository;
import com.project.vinylata.Repository.CartRepository;
import com.project.vinylata.Repository.ProductRepository;
import com.project.vinylata.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CartService implements ICartService{
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository  cartItemRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;


    @Override
    public CartDto listCartItems(Cart cart) {
        List<CartItem> cartItemList = cartItemRepository.findByCart(cart);
        CartDto cartDto = new CartDto();
        List<CartItemDto> cartItemDtos = new ArrayList<>();
        double totalCost = 0;
        for (CartItem ci: cartItemList) {
            CartItemDto cartItemDto = new CartItemDto();
            cartItemDto.setId(ci.getId());
            cartItemDto.setProductId(ci.getProduct().getId());
            cartItemDto.setProductTitle(ci.getProduct().getProductTitle());
            cartItemDto.setProductImage(ci.getProduct().getProductImage());
            cartItemDto.setUnitPrice(productRepository.findById(ci.getProduct().getId()).getProductPricing());
            cartItemDto.setQuantity(ci.getQuantity());
            cartItemDto.setTotalPaymentEachCartItem(ci.getQuantity()*ci.getProduct().getProductPricing());
            cartItemDtos.add(cartItemDto);
            totalCost += ci.getQuantity()*ci.getProduct().getProductPricing();
        }
        cartDto.setCartItemDtos(cartItemDtos);
        cartDto.setTotalMoney(totalCost);
        return cartDto;
    }

    @Override
    public void addToCart(AddToCartDto addToCartDto, HttpServletRequest request, User user) {
        //localStorage
        if (user == null){
            HttpSession session = request.getSession();
            CartDto cartDto = (CartDto) session.getAttribute("cart");
            if (cartDto != null){
                List<CartItemDto> cartItemDtos = cartDto.getCartItemDtos();
                //check if productId has existed in cartItem
                for (CartItemDto c: cartItemDtos){
                    if (c.getProductId() == addToCartDto.getProductId()){
                        c.setQuantity(c.getQuantity()+1);
                        c.setTotalPaymentEachCartItem(c.getQuantity() * c.getUnitPrice());
                        cartDto.setTotalMoney(calculateTotalCost(cartDto));
                        session.setAttribute("cart", cartDto);
                        return;
                    }
                }
                // add new cartItem to existed cart
                CartItemDto cartItemDto = new CartItemDto();
                cartItemDto.setProductId(addToCartDto.getProductId());
                cartItemDto.setProductImage(productRepository.findById(addToCartDto.getProductId()).getProductImage());
                cartItemDto.setProductTitle(productRepository.findById(addToCartDto.getProductId()).getProductTitle());
                cartItemDto.setQuantity(1);
                cartItemDto.setUnitPrice(productRepository.findById(addToCartDto.getProductId()).getProductPricing());
                cartItemDto.setTotalPaymentEachCartItem(cartItemDto.getQuantity() * cartItemDto.getUnitPrice());
                cartItemDtos.add(cartItemDto);
                cartDto.setCartItemDtos(cartItemDtos);
                cartDto.setTotalMoney(calculateTotalCost(cartDto));
                session.setAttribute("cart", cartDto);
            }else {
                CartDto cartDto1 = new CartDto();
                CartItemDto cartItemDto = new CartItemDto();
                List<CartItemDto> cartItemDtos = new ArrayList<>();
                cartItemDto.setProductId(addToCartDto.getProductId());
                cartItemDto.setQuantity(1);
                cartItemDto.setProductImage(productRepository.findById(addToCartDto.getProductId()).getProductImage());
                cartItemDto.setProductTitle(productRepository.findById(addToCartDto.getProductId()).getProductTitle());
                cartItemDto.setUnitPrice(productRepository.findById(addToCartDto.getProductId()).getProductPricing());
                cartItemDto.setTotalPaymentEachCartItem(cartItemDto.getQuantity() * cartItemDto.getUnitPrice());
                cartItemDtos.add(cartItemDto);
                cartDto1.setCartItemDtos(cartItemDtos);
                cartDto1.setTotalMoney(calculateTotalCost(cartDto1));
                session.setAttribute("cart", cartDto1);
            }
        }
        //database
        else {
            Cart cart = cartRepository.findCartByUser(user);
            if (cart == null) {
                cart = new Cart();
                cart.setUser(user);
                cartRepository.save(cart);
            }

            Optional<Product> product = Optional.ofNullable(productRepository.findById(addToCartDto.getProductId()));
            if (product.isEmpty()) {
                throw new ProductNotExistException("this product has not existed");
            }
            //handle if product has been in cart already
            CartItem cartItem = cartItemRepository.findByProductAndCart(product.get(), cart);
            if (cartItem != null) {
                cartItem.setQuantity(cartItem.getQuantity() + 1);
                cartItemRepository.save(cartItem);
            } else {
                cartItem = new CartItem();
                cartItem.setCart(cart);
                cartItem.setProduct(product.get());
                cartItem.setQuantity(1);
                cartItemRepository.save(cartItem);
            }
        }
    }

    private double calculateTotalCost(CartDto cartDto){
        List<CartItemDto> cartItemDtos = cartDto.getCartItemDtos();
        double total = 0;
        for (CartItemDto c: cartItemDtos){
            total += c.getQuantity() * c.getUnitPrice();
        }
        return total;
    }

    public CartDto deleteProductFromLocalStorage(CartDto cartDto, long productId){
        List<CartItemDto> cartItemDtoList = cartDto.getCartItemDtos();
        for (CartItemDto cartItemDto: cartItemDtoList){
            if (cartItemDto.getProductId() == productId){
                cartItemDtoList.remove(cartItemDto);
            }
        }
        cartDto.setCartItemDtos(cartItemDtoList);
        cartDto.setTotalMoney(calculateTotalCost(cartDto));
        return cartDto;
    }

    public CartDto changeQuantityInLocalStorage(CartDto cartDto, int quantity, String id){
        List<CartItemDto> cartItemDtoList = cartDto.getCartItemDtos();
        for (CartItemDto cartItemDto: cartItemDtoList){
            if (cartItemDto.getProductId() == Long.parseLong(id)){
                cartItemDto.setQuantity(quantity);
                cartItemDto.setTotalPaymentEachCartItem(cartItemDto.getQuantity() * cartItemDto.getUnitPrice());
            }
        }
        cartDto.setCartItemDtos(cartItemDtoList);
        cartDto.setTotalMoney(calculateTotalCost(cartDto));
        return cartDto;
    }

}
