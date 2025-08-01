import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrls: ['./login.css']
})
export class LoginComponent {
  loginForm: FormGroup;
  registerForm: FormGroup;
  isLoginView = true;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  constructor(private fb: FormBuilder, private authService: AuthService) {
    this.loginForm = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });

    this.registerForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [Validators.required, Validators.minLength(6)]]
    });
  }

  toggleView(): void {
    this.isLoginView = !this.isLoginView;
    this.errorMessage = null;
    this.successMessage = null;
  }

  onLogin(): void {
    if (this.loginForm.valid) {
      this.authService.login(this.loginForm.value).subscribe({
        error: (err) => this.errorMessage = err.error.message || 'Login failed.'
      });
    }
  }

  onRegister(): void {
    if (this.registerForm.valid) {
      this.successMessage = null;
      this.errorMessage = null;
      this.authService.register(this.registerForm.value).subscribe({
        next: () => {
          this.successMessage = "Registration successful! Please log in.";
          this.toggleView();
        },
        error: (err) => this.errorMessage = err.error.message || 'Registration failed.'
      });
    }
  }
}