import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ApiService } from '../../services/api';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CurrencyPipe, NgxChartsModule, RouterModule],
  templateUrl: './home.html',
  styleUrls: ['./home.css']
})
export class HomeComponent implements OnInit {
  portfolio: any;
  holdings: any[] = [];
  portfolioHistory: any[] = [];
  isLoading = true;

  // ngx-charts options
  colorScheme: any = {
    domain: ['#00C805']
  };

  constructor(private apiService: ApiService) { }

  ngOnInit(): void {
    this.apiService.getPortfolio().subscribe(data => {
      this.portfolio = data;
      this.holdings = data.holdings;
      
      if (data && data.history && data.history.length > 0) {
        this.portfolioHistory = [{
          name: 'Portfolio',
          series: data.history.map((h: { time: any; value: any; }) => ({ 
              name: new Date(h.time * 1000), 
              value: h.value 
          }))
        }];
      } else {
        this.portfolioHistory = [];
      }
      this.isLoading = false;
    });
  }

  // Custom function to format Y-axis ticks as currency
  yAxisTickFormatting(val: any): string {
    return `$${val.toLocaleString()}`;
  }
}