package com.ecommerce.order.dto;

import lombok.Data;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class CreateOrderDTO {

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    private Integer quantity;

    @NotNull(message = "地址ID不能为空")
    private Long addressId;

    private String remark;

    @NotBlank(message = "幂等性Token不能为空")
    private String idempotentToken;
}
