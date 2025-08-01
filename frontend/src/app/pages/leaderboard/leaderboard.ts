import { CommonModule, CurrencyPipe } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ApiService } from '../../services/api';

@Component({
  selector: 'app-leaderboard',
  standalone: true,
  imports: [CommonModule, CurrencyPipe],
  templateUrl: './leaderboard.html',
  styleUrls: ['./leaderboard.css']
})
export class LeaderboardComponent implements OnInit {
  leaderboardData: any[] = [];
  isLoading = true;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.apiService.getLeaderboard().subscribe(data => {
      this.leaderboardData = data;
      this.isLoading = false;
    });
  }
}