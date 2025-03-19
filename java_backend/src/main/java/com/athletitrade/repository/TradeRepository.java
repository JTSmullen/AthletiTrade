// TradeRepository.java
package com.athletitrade.repository;
import com.athletitrade.model.Player;
import com.athletitrade.model.Trade;
import com.athletitrade.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserId(Long userId);
    List<Trade> findByPlayerId(Integer playerId);
    List<Trade> findByUserAndPlayerOrderByTimestampDesc(User user, Player player);

}