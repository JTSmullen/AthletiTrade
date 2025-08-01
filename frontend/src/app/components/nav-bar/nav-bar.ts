import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { Observable, Subject } from 'rxjs';
import {
  debounceTime, distinctUntilChanged, switchMap
} from 'rxjs/operators';
import { ApiService } from '../../services/api';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-nav-bar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './nav-bar.html',
  styleUrls: ['./nav-bar.css']
})
export class NavBarComponent implements OnInit {
  searchResults$!: Observable<string[]>;
  private searchTerms = new Subject<string>();
  showResults = false;

  constructor(
    private authService: AuthService, 
    private apiService: ApiService,
    private router: Router
  ) {}

  search(event: Event): void {
    const term = (event.target as HTMLInputElement).value;
    this.showResults = !!term;
    this.searchTerms.next(term);
  }

  ngOnInit(): void {
    this.searchResults$ = this.searchTerms.pipe(

      debounceTime(300),

      distinctUntilChanged(),
      switchMap((term: string) => this.apiService.searchPlayers(term)),
    );
  }

  selectPlayer(playerId: string, searchInput: HTMLInputElement): void {
    this.showResults = false;
    searchInput.value = '';
    this.searchTerms.next('');
    this.router.navigate(['/player', playerId]);
  }

  onLogout(): void {
    this.authService.logout();
  }
}