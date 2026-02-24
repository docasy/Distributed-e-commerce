package com.ecommerce.order.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 创建订单DTO
 */
@Data
public class CreateOrderDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    @NotBlank(message = "收货地址不能为空")
    private String address;

    @NotBlank(message = "收货人不能为空")
    private String receiver;

    @NotBlank(message = "收货人电话不能为空")
    private String receiverPhone;

    private String remark;

    /**
     * 幂等性Token
     */
    @NotBlank(message = "幂等性Token不能为空")
    private String idempotentToken;
}
