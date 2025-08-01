import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = 'http://127.0.0.1:5001/auth'; // Your Flask backend URL + /auth prefix
  private _isLoggedIn$ = new BehaviorSubject<boolean>(false);
  isLoggedIn$ = this._isLoggedIn$.asObservable();

  constructor(private http: HttpClient, private router: Router) {
    const token = this.getToken();
    this._isLoggedIn$.next(!!token);
  }

  getToken(): string | null {
    return localStorage.getItem('authToken');
  }

  register(credentials: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, credentials);
  }

  login(credentials: any): Observable<any> {
    return this.http.post<{token: string}>(`${this.apiUrl}/login`, credentials).pipe(
      tap(({ token }) => {
        localStorage.setItem('authToken', token);
        this._isLoggedIn$.next(true);
        this.router.navigate(['/home']);
      })
    );
  }

  logout(): void {
    localStorage.removeItem('authToken');
    this._isLoggedIn$.next(false);
    this.router.navigate(['/login']);
  }
}