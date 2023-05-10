package com.project.vinylata.Controller;

import com.project.vinylata.DTO.CartDto;
import com.project.vinylata.DTO.OrderDto;
import com.project.vinylata.DTO.OrderItemDto;
import com.project.vinylata.DTO.VoucherDto;
import com.project.vinylata.Model.*;
import com.project.vinylata.Repository.*;
import com.project.vinylata.Response.ResponseHandler;
import com.project.vinylata.Service.CartService;
import com.project.vinylata.Service.EmailService;
import com.project.vinylata.Service.ProductService;
import com.project.vinylata.Service.VoucherService;
import jakarta.annotation.Nullable;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@RequestMapping("/api/order/user")
@CrossOrigin(origins = "http://localhost:9000")
@RestController
public class CheckoutController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VoucherService voucherService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/checkout")
    public ResponseEntity<Object> checkOutPage(HttpServletRequest request){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByEmail(username);
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
        return ResponseHandler.responseBuilder("success", HttpStatus.ACCEPTED, cartDto);
    }

    @PostMapping("/confirm")
    public ResponseEntity<Object> confirm(@RequestBody OrderDto orderDto){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<User> user = userRepository.findByEmail(username);

        Order order = new Order();
        orderRepository.save(order);
        List<OrderItemDto> orderItemDto =  orderDto.getItemDtoList();
        List<OrderItem> orderItems = new ArrayList<>();
        double totalCost = 0;

        for (OrderItemDto each: orderItemDto) {
            OrderItem orderItem = new OrderItem();
//            orderItem.setId(each.getId());
            orderItem.setProduct(productRepository.findById(each.getProductId()));
            orderItem.setAmount(each.getAmount());
            orderItem.setTotalPaymentEachOrderItem(each.getTotalPaymentEachOrderItem());
            orderItem.setOrder(order);
            orderItems.add(orderItem);
            totalCost += each.getTotalPaymentEachOrderItem();
            orderItemRepository.save(orderItem);
        }

        //order.setId(orderDto.getId());
        order.setOrderStatus("waiting for confirmation....");
        order.setAddress(orderDto.getAddress());
        order.setRecipientPhoneNo(orderDto.getRecipientPhoneNo());
        order.setEmail(orderDto.getEmail());
        order.setCreatedDate(new Date());
        if (user.isEmpty()){
            order.setTotalPayment(totalCost);
            order.setUser(null);
            orderRepository.save(order);
            return ResponseHandler.responseBuilder("oke", HttpStatus.OK, "order has been confirmed to process!");
        }
        order.setTotalPayment(totalCost - totalCost*(voucherService.getDiscountById(orderDto.getVoucherId())));
        order.setUser(user.get());
        orderRepository.save(order);

        //and remove voucher from cart
        Cart cart = cartRepository.findCartByUser(user.get());
        cart.setVoucher(null);
        cartRepository.save(cart);

        //minus 1 out of quantity
        voucherService.decreaseQuantity(orderDto.getVoucherId());

        // Process the order and generate orderDetails string
        StringBuilder orderItemList = new StringBuilder();
        for (OrderItemDto orderItem: orderDto.getItemDtoList()){
            orderItemList.append(orderItem.getProductTitle());
            orderItemList.append(", ");
        }

        String orderDetails = "Order ID: " + order.getId() +
                "<br/>" + "Order item list: " + orderItemList +
                "<br/>" + "Order Payment: " + order.getTotalPayment() +
                "<br/>" + "Order Address: " + order.getAddress() +
                "<br/>" + "Order Date: " + order.getCreatedDate();
        // Send email to the customer
        try {
            emailService.sendOrderConfirmationEmail(orderDto.getEmail(), orderDetails);
        } catch (MessagingException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Failed to send email", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return ResponseHandler.responseBuilder("oke", HttpStatus.OK, "order has been confirmed to process!");
    }
}
