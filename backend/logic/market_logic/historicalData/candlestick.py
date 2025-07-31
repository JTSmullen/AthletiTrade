import time
from datetime import datetime, timedelta
from dataclasses import dataclass, field

@dataclass
class Candlestick:
    timestamp: int
    open: float
    high: float
    low: float
    close: float
    volume: int