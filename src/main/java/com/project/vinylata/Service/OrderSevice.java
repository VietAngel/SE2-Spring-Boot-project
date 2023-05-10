package com.project.vinylata.Service;

import com.project.vinylata.DTO.ManagedOrderDto;
import com.project.vinylata.DTO.OrderItemDto;
import com.project.vinylata.Exception.EmailMessageException;
import com.project.vinylata.Model.Order;
import com.project.vinylata.Model.OrderItem;
import com.project.vinylata.Model.User;
import com.project.vinylata.Repository.OrderRepository;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderSevice {

    @Autowired
    private EmailService emailService;

    @Autowired
    private OrderRepository orderRepository;

    public List<ManagedOrderDto> getMyOrder(User user){
        List<Order> orders = orderRepository.findByUser(user);
        return filter(orders);
    }

    public List<ManagedOrderDto> getUnconfirmedOrder(){
        List<Order> orders = orderRepository.findByOrderStatus("waiting for confirmation....");
        return filter(orders);
    }

    public List<ManagedOrderDto> getConfirmedOrder(){
        List<Order> orders = orderRepository.findByOrderStatus("confirmed");
        return filter(orders);
    }

    public void acceptOrder(long id){
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()){
            //handle exception
        }
        Order findedOrder = order.get();
        if (order.get().getOrderStatus().equals("waiting for confirmation....")){
            //handle exception
        }
        findedOrder.setOrderStatus("confirmed");

        //handle send email
        StringBuilder orderItemList = new StringBuilder();
        for (OrderItem orderItem: findedOrder.getItemList()){
            orderItemList.append(orderItem.getProduct().getProductTitle());
            orderItemList.append(", ");
        }

        String orderDetails = "Order ID: " + findedOrder.getId() +
                "<br/>" + "Order item list: " + orderItemList +
                "<br/>" + "Order Payment: " + findedOrder.getTotalPayment() +
                "<br/>" + "Order Address: " + findedOrder.getAddress() +
                "<br/>" + "Order Phone Number: " + findedOrder.getRecipientPhoneNo() +
                "<br/>" + "Order Date: " + findedOrder.getCreatedDate();
        // Send email to the customer
        try {
            emailService.sendOfficialOrderConfirmationEmail(findedOrder.getEmail(), orderDetails);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new EmailMessageException("failed to send message to customer!");
        }

        orderRepository.save(findedOrder);
    }

    public void cancelOrderById(long id){
        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()){
            //handle exception
        }
        Order findedOrder = order.get();
        if (order.get().getOrderStatus().equals("waiting for confirmation....")){
            //handle exception
        }
        findedOrder.setOrderStatus("canceled");
        //handle send email
        StringBuilder orderItemList = new StringBuilder();
        for (OrderItem orderItem: findedOrder.getItemList()){
            orderItemList.append(orderItem.getProduct().getProductTitle());
            orderItemList.append(", ");
        }

        String orderDetails = "Order ID: " + findedOrder.getId() +
                "<br/>" + "Order item list: " + orderItemList +
                "<br/>" + "Order Payment: " + findedOrder.getTotalPayment() +
                "<br/>" + "Order Address: " + findedOrder.getAddress() +
                "<br/>" + "Order Phone Number: " + findedOrder.getRecipientPhoneNo() +
                "<br/>" + "Order Date: " + findedOrder.getCreatedDate();
        // Send email to the customer
        try {
            emailService.sendCanceledOrderConfirmationEmail(findedOrder.getEmail(), orderDetails);
        } catch (MessagingException e) {
            e.printStackTrace();
            throw new EmailMessageException("failed to send message to customer!");
        }

        orderRepository.save(findedOrder);
    }

    public void cancelOrderByUserAndId(User user, long id){

        Order order = orderRepository.findOrderByUserAndId(user, id);
        if (order == null){
            //handle exception
        }
        if (order.getOrderStatus().equals("waiting for confirmation....")){
            //handle exception
        }
        order.setOrderStatus("canceled");
        orderRepository.save(order);
    }

    private List<ManagedOrderDto> filter(List<Order> orders){
        List<ManagedOrderDto> managedOrderDtoList = new ArrayList<>();
        for (Order order: orders){
            ManagedOrderDto managedOrderDto = new ManagedOrderDto();
            managedOrderDto.setId(order.getId());
            managedOrderDto.setAddress(order.getAddress());
            List<OrderItem> orderItems = order.getItemList();
            List<OrderItemDto> orderItemDtos = new ArrayList<>();
            for (OrderItem orderItem: orderItems){
                orderItemDtos.add(new OrderItemDto(orderItem.getId(),
                        orderItem.getProduct().getId(),
                        orderItem.getProduct().getProductTitle(),
                        orderItem.getProduct().getProductImage(),
                        orderItem.getAmount(),
                        orderItem.getTotalPaymentEachOrderItem()));
            }
            managedOrderDto.setItemDtoList(orderItemDtos);
            managedOrderDto.setAddress(order.getAddress());
            managedOrderDto.setEmail(order.getEmail());
            managedOrderDto.setRecipientPhoneNo(order.getRecipientPhoneNo());
            managedOrderDto.setOrderStatus(order.getOrderStatus());
            managedOrderDto.setTotalPayment(order.getTotalPayment());

            //add to list
            managedOrderDtoList.add(managedOrderDto);
        }
        return managedOrderDtoList;
    }
}
