//PlayerPriceHistoryRepository.java
package com.athletitrade.repository;

import com.athletitrade.model.PlayerPriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PlayerPriceHistoryRepository extends JpaRepository<PlayerPriceHistory, Long> {
    List<PlayerPriceHistory> findByPlayerIdOrderByTimestampDesc(Integer playerId);
}