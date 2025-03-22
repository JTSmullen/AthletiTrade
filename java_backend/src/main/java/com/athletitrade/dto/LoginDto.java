// LoginDto.java
package com.athletitrade.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class LoginDto {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public String getUsername(){
        return username;
    }

    public String getPassword(){
        return password;
    }
    public void setPassword(String password){this.password = password;}
    public void setUsername(String username){this.username = username;}
}