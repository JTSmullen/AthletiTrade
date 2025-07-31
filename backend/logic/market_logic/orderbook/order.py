import time
from dataclasses import dataclass, field

@dataclass
class Order:
    user_id: str
    player_id: str
    side: str # buy or sell
    price: float
    quantity: int
    order_id: str | None = None
    timestamp: float = field(default_factory=time.time)

    def __lt__(self, other):
        return self.price < other.price