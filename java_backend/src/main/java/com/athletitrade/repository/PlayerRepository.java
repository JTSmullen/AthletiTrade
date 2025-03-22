// PlayerRepository.java
package com.athletitrade.repository;

import com.athletitrade.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PlayerRepository extends JpaRepository<Player, Integer> {
    Optional<Player> findByFullName(String fullName);
    List<Player> findByFullNameContainingIgnoreCase(String query);
}