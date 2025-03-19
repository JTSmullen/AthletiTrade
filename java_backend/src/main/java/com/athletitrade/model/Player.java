// Player.java
package com.athletitrade.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "players")
public class Player {
    @Id
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    public String getFullName(){return fullName;}

    public Integer getId(){return id;}

    // Other static player info (team, position, etc.) - if needed
}