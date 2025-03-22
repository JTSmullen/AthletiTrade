// BuyOrderDto.java
package com.athletitrade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class BuyOrderDto {
    @NotNull(message = "Player ID is required")
    private Integer playerId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public Integer getPlayerId(){
        return playerId;
    }

    public Integer getQuantity(){
        return quantity;
    }
    public void setPlayerId(Integer playerId){this.playerId = playerId;}
    public void setQuantity(Integer quantity){this.quantity = quantity;}
}