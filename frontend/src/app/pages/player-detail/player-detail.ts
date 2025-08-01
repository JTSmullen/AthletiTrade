import { CommonModule, CurrencyPipe, KeyValue } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { forkJoin } from 'rxjs';
import { ApiService } from '../../services/api';

// --- Interface Definitions ---
interface Holding {
  player_id: string;
  quantity: number;
  avg_cost: number;
  market_value: number;
}

interface Portfolio {
  holdings: Holding[];
  cash_balance: number;
  total_value: number;
}

@Component({
  selector: 'app-player-detail',
  standalone: true,
  imports: [
    CommonModule,
    CurrencyPipe,
    NgxChartsModule,
    ReactiveFormsModule
  ],
  templateUrl: './player-detail.html',
  styleUrls: ['./player-detail.css']
})
export class PlayerDetailComponent implements OnInit {
  @Input() id = ''; 
  
  playerHistory: any[] = [];
  orderBook: any;
  openOrders: any[] = [];
  currentPrice: number = 0;
  
  currentUserHolding: any = null;
  lifetimeStats: any = null;
  
  isLoading = true;
  isOrderBookView = false;
  
  orderForm: FormGroup;
  orderMessage: string | null = null;
  orderMessageType: 'success' | 'error' = 'success';

  colorScheme: any = { domain: ['#0A84FF'] };

  constructor(
    private apiService: ApiService,
    private fb: FormBuilder
  ) {
    this.orderForm = this.fb.group({
      quantity: [null, [Validators.required, Validators.min(1)]],
      price: [null, [Validators.required, Validators.min(0.01)]],
      side: ['buy', Validators.required]
    });
  }

  ngOnInit(): void {
    this.fetchData();
  }

  fetchData(): void {
    this.isLoading = true;
    forkJoin({
      marketHistory: this.apiService.getPlayerHistory(this.id),
      orderBook: this.apiService.getPlayerOrderBook(this.id),
      openOrders: this.apiService.getOpenOrders(),
      portfolio: this.apiService.getPortfolio(),
      lifetimeStats: this.apiService.getPlayerLifetimeStats(this.id)
    }).subscribe(({ marketHistory, orderBook, openOrders, portfolio, lifetimeStats }) => {
      this.currentPrice = marketHistory.current_price || 0;
      this.orderForm.patchValue({ price: this.currentPrice });

      if (marketHistory.prices && marketHistory.prices.length > 0) {
        this.playerHistory = [{
          name: this.id,
          series: marketHistory.prices.map((p: { time: any; price: any; }) => ({ 
              name: new Date(p.time * 1000), 
              value: p.price 
          }))
        }];
      } else {
          this.playerHistory = [];
      }
      
      this.orderBook = orderBook;
      this.openOrders = openOrders.filter(order => order.player_id === this.id);
      
      this.calculateCurrentPositionStats(portfolio);
      this.calculateLifetimeStats(portfolio, lifetimeStats);

      this.isLoading = false;
    });
  }

  // --- NEW SORTING FUNCTIONS FOR THE ORDER BOOK ---
  // For Bids (highest price first)
  descendingOrder = (a: KeyValue<string, number>, b: KeyValue<string, number>): number => {
    return parseFloat(b.key) - parseFloat(a.key);
  }

  // For Asks (lowest price first)
  ascendingOrder = (a: KeyValue<string, number>, b: KeyValue<string, number>): number => {
    return parseFloat(a.key) - parseFloat(b.key);
  }
  // --- END NEW FUNCTIONS ---

  calculateCurrentPositionStats(portfolio: Portfolio): void {
    const holding = portfolio.holdings.find((h: Holding) => h.player_id === this.id);
    if (holding && holding.quantity > 0) {
      const marketValue = holding.quantity * this.currentPrice;
      const totalCost = holding.quantity * holding.avg_cost;
      const gainLoss = marketValue - totalCost;
      const percentChange = totalCost > 0 ? (gainLoss / totalCost) * 100 : 0;
      this.currentUserHolding = {
        avg_cost: holding.avg_cost,
        market_value: marketValue,
        percent_change: percentChange,
        gain_loss: gainLoss
      };
    } else {
      this.currentUserHolding = null;
    }
  }

  calculateLifetimeStats(portfolio: Portfolio, lifetimeData: any): void {
    const holding = portfolio.holdings.find((h: Holding) => h.player_id === this.id);
    const currentMarketValue = holding ? holding.quantity * this.currentPrice : 0;
    const totalCost = lifetimeData.total_cost || 0;
    const totalProceeds = lifetimeData.total_proceeds || 0;

    if (totalCost > 0 || totalProceeds > 0) {
      const totalReturn = (totalProceeds + currentMarketValue) - totalCost;
      const totalReturnPercent = totalCost > 0 ? (totalReturn / totalCost) * 100 : 0;
      this.lifetimeStats = {
        total_return: totalReturn,
        total_return_percent: totalReturnPercent
      };
    } else {
      this.lifetimeStats = null;
    }
  }
  
  onCancelOrder(orderId: number): void {
    this.apiService.cancelOrder(orderId).subscribe({
      next: () => { this.fetchData(); },
      error: (err) => {
        this.orderMessage = err.error.error || 'Failed to cancel order.';
        this.orderMessageType = 'error';
        setTimeout(() => this.orderMessage = null, 5000);
      }
    });
  }

  toggleChart(isOrderBook: boolean): void {
    this.isOrderBookView = isOrderBook;
  }
  
  setOrderSide(side: 'buy' | 'sell'): void {
    this.orderForm.patchValue({ side: side });
  }

  placeOrder(): void {
    if (this.orderForm.invalid) return;
    const orderData = { player_id: this.id, ...this.orderForm.value };
    
    this.apiService.placeOrder(orderData).subscribe({
      next: () => {
        this.orderMessage = 'Order placed successfully!';
        this.orderMessageType = 'success';
        this.orderForm.reset({ side: this.orderForm.value.side, price: this.currentPrice, quantity: null });
        setTimeout(() => this.orderMessage = null, 3000);
        this.fetchData(); 
      },
      error: (err) => {
        this.orderMessage = err.error.error || 'Failed to place order.';
        this.orderMessageType = 'error';
        setTimeout(() => this.orderMessage = null, 5000);
      }
    });
  }
}