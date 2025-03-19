// PlayerPriceHistory.java
package com.athletitrade.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "player_price_history")
public class PlayerPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable=false)
    private Player player;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Double price;

    public Double getPrice(){return price;}

    public void setPlayer(Player player){this.player = player;}

    public void setPrice(Double price){this.price = price;}

    public void setTimestamp(LocalDateTime timestamp){this.timestamp = timestamp;}
}