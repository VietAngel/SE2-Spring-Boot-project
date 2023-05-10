package com.project.vinylata.DTO;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManagedOrderDto {
    private long id;

    private List<OrderItemDto> itemDtoList;

    private double totalPayment;

    private String orderStatus;

    private String address;

    private String recipientPhoneNo;
    @Email
    private String email;
}
