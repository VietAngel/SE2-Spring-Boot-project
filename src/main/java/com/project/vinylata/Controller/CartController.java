package com.project.vinylata.Controller;

import com.project.vinylata.DTO.AddToCartDto;
import com.project.vinylata.DTO.CartDto;
import com.project.vinylata.Exception.CartItemNotExistException;
import com.project.vinylata.Model.*;
import com.project.vinylata.Repository.CartItemRepository;
import com.project.vinylata.Repository.CartRepository;
import com.project.vinylata.Repository.ProductRepository;
import com.project.vinylata.Repository.UserRepository;
import com.project.vinylata.Response.ResponseHandler;
import com.project.vinylata.Service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin(origins = "http://localhost:9000")
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CartItemService cartItemService;

    @Autowired
    private VoucherService voucherService;

    @GetMapping("/")
    public ResponseEntity<Object> cartPage(HttpServletResponse response, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByEmail(username);
        //localStorage
        if (user.isEmpty()){
            HttpSession session = request.getSession();
            CartDto localStorageCartDto = (CartDto)session.getAttribute("cart");
            return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, localStorageCartDto);
        }

        Cart cart = cartRepository.findCartByUser(user.get());
        if(cart==null) {
            cart = new Cart();
            cart.setUser(userRepository.getUsersByEmail(username));
            cartRepository.save(cart);
        }
        CartDto cartDto = cartService.listCartItems(cart);
        if (cart.getVoucher() == null){
            return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, cartDto);
        }
        cartDto.setVoucherId(cart.getVoucher().getId());
        cartDto.setTotalMoney(cartDto.getTotalMoney() - cartDto.getTotalMoney() * voucherService.getDiscountById(cart.getVoucher().getId()));
        //oke done
        return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, cartDto);
    }

    @PostMapping("/voucher/select/{id}")
    public ResponseEntity<Object> selectVoucher(@PathVariable long id){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByEmail(username);
        Cart cart = cartRepository.findCartByUser(user.get());
        if(cart==null) {
            cart = new Cart();
            cart.setUser(userRepository.getUsersByEmail(username));
            cartRepository.save(cart);
        }
        Voucher voucher = voucherService.findVoucherById(id);
        cart.setVoucher(voucher);
        cartRepository.save(cart);

        return ResponseHandler.responseBuilder("oke", HttpStatus.OK, "voucher has been selected");
    }

    @GetMapping("/addToCart")
    public ResponseEntity<Object> addToCart(@RequestBody AddToCartDto addToCartDto, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser  = userRepository.getUsersByEmail(username);
        cartService.addToCart(addToCartDto, request, currentUser);
        return ResponseHandler.responseBuilder("oke", HttpStatus.OK, "product has been added to cart");
    }

    @PutMapping("/changCartItemQuanity/{productId}")
    public ResponseEntity<Object> changeQuanity(@PathVariable String productId,@RequestParam String quantity, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser  = userRepository.getUsersByEmail(username);
        if (currentUser == null){
            HttpSession session = request.getSession();
            CartDto localStorageCartDto = (CartDto)session.getAttribute("cart");
            //and change quantity
            CartDto cartDto = cartService.changeQuantityInLocalStorage(localStorageCartDto, Integer.parseInt(quantity), productId);
            session.setAttribute("cart", cartDto);
            return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, "has been removed");
        }


        Cart cart = cartRepository.getCartByUser(currentUser);
        Product product = productRepository.findById(Long.parseLong(productId));
        CartItem cartItem = cartItemRepository.findByProductAndCart(product, cart);
        if (cartItem == null){
            throw new CartItemNotExistException("this is not found");
        }
        cartItem.setQuantity(Integer.parseInt(quantity));
        cartItemRepository.save(cartItem);
        return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, "this quantity has been changed successfully");
    }

    @PostMapping("/deleteCartItem/{productId}")
    public ResponseEntity<Object> deleteCartItem(@PathVariable String productId, HttpServletRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser  = userRepository.getUsersByEmail(username);
        if (currentUser == null){
            HttpSession session = request.getSession();
            CartDto localStorageCartDto = (CartDto)session.getAttribute("cart");
            //and delete cartItem
            CartDto cartDto = cartService.deleteProductFromLocalStorage(localStorageCartDto, Long.parseLong(productId));
            session.setAttribute("cart", cartDto);
            return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, "has been removed");
        }

        Cart cart = cartRepository.getCartByUser(currentUser);
        Product product = productRepository.findById(Long.parseLong(productId));
        CartItem cartItem = cartItemRepository.findByProductAndCart(product,cart);

        if (cartItem == null){
            throw new CartItemNotExistException("this is not found");
        }

        cartItemRepository.delete(cartItem);

        return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, "cartItem"+ cartItem + "is deleted from cart");
    }
}
