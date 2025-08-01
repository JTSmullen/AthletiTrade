import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // The base URL remains the same
  private apiUrl = 'http://127.0.0.1:5001';

  constructor(private http: HttpClient) { }

  // Portfolio Endpoints
  getPortfolio(): Observable<any> {
    // Add the '/api' prefix
    return this.http.get(`${this.apiUrl}/api/portfolio`);
  }

  placeOrder(orderData: any): Observable<any> {
    // Add the '/api' prefix
    return this.http.post(`${this.apiUrl}/api/orders`, orderData);
  }

  // Market Endpoints
  getAllPlayers(): Observable<any> {
    // Add the '/market' prefix
    return this.http.get(`${this.apiUrl}/market/players`);
  }
  
  getPlayerHistory(playerId: string): Observable<any> {
    // Add the '/market' prefix
    return this.http.get(`${this.apiUrl}/market/history/${playerId}`);
  }

  getPlayerOrderBook(playerId: string): Observable<any> {
    // Add the '/market' prefix
    return this.http.get(`${this.apiUrl}/market/orderbooks/${playerId}`);
  }

  // Leaderboard Endpoint
  getLeaderboard(): Observable<any> {
    // Keep the '/leaderboard' prefix (assuming the route in the file is just '/')
    // If the route in leaderboard_routes.py is also '/leaderboard', then this URL is correct.
    return this.http.get(`${this.apiUrl}/leaderboard/leaderboard`);
  }

  searchPlayers(term: string): Observable<string[]> {
    if (!term.trim()) {
      // if not search term, return empty array.
      return of([]);
    }
    // Note: We use `of` from rxjs if the term is empty
    return this.http.get<string[]>(`${this.apiUrl}/market/players/search`, {
      params: { q: term }
    });
  }

  getOpenOrders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/api/orders`);
  }

  cancelOrder(orderId: number): Observable<any> {
    return this.http.delete(`${this.apiUrl}/api/orders/${orderId}`);
  }

  getPlayerLifetimeStats(playerId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/api/portfolio/history/${playerId}`);
  }
}