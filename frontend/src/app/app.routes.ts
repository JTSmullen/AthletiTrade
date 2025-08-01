import { Routes } from '@angular/router';
import { authGuard } from './guards/auth-guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login').then(m => m.LoginComponent)
  },
  {
    path: 'home',
    loadComponent: () => import('./pages/home/home').then(m => m.HomeComponent),
    canActivate: [authGuard]
  },
  {
    path: 'leaderboard',
    loadComponent: () => import('./pages/leaderboard/leaderboard').then(m => m.LeaderboardComponent),
    canActivate: [authGuard]
  },
  {
    path: 'player/:id', // The router will automatically bind 'id' to the component's @Input()
    loadComponent: () => import('./pages/player-detail/player-detail').then(m => m.PlayerDetailComponent),
    canActivate: [authGuard]
  },
  { path: '', redirectTo: 'home', pathMatch: 'full' },
  { path: '**', redirectTo: 'home' } // Or a dedicated 404 component
];