CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Store hashed passwords!
    email VARCHAR(100) UNIQUE,
    balance DECIMAL(10, 2) NOT NULL DEFAULT 10000.00 -- Starting funds
);

CREATE TABLE players (
    player_id INT PRIMARY KEY, -- NBA Player ID
    player_name VARCHAR(100) NOT NULL,
    team_id INT, -- NBA Team ID
    position VARCHAR(50),
    current_price DECIMAL(10, 2) NOT NULL DEFAULT 10.00 -- Initial price / This will very for each player
);

CREATE TABLE trades (
    trade_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    player_id INT NOT NULL,
    trade_type VARCHAR(10) NOT NULL, -- 'BUY' or 'SELL'
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    trade_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (player_id) REFERENCES players(player_id)
);

CREATE TABLE player_price_history (
    price_history_id INT AUTO_INCREMENT PRIMARY KEY,
    player_id INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (player_id) REFERENCES players(player_id)
);