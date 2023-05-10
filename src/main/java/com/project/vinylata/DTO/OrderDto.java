package com.project.vinylata.DTO;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.vinylata.Model.User;
import com.project.vinylata.Model.Voucher;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {
    private long id;

    private List<OrderItemDto> itemDtoList;

    private String address;

    private String recipientPhoneNo;
    @Email
    private String email;

    private long voucherId;

}
