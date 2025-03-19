//PlayerStatsDto.java
package com.athletitrade.dto;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PlayerStatsDto {
    private Double weighted_average;
    private String gamelog; // Keep this as String for now

    // Assuming these fields exist in the JSON you get from Python
    private Double PTS;
    private Double REB;
    private Double AST;
    private Double TOV; //Needed for calculatePrice

    public Double getPTS() {
        return PTS;
    }

    public Double getREB() {
        return REB;
    }

    public  Double getAST() {
        return AST;
    }
    public Double getTOV(){
        return TOV;
    }

    public Double getWeightedAverage() {
        return weighted_average;
    }

    //There is no Id to get here, this is a DTO of stats, not the player.
}