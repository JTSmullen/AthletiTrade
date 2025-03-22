// src/components/Header.tsx
import React from 'react';

function Header() {
  return (
    <header>
      <h1>AthletiTrade</h1>
      <nav>
        <ul>
          <li><a href="/">Home</a></li>
          <li><a href="/login">Login</a></li>
          <li><a href="/register">Register</a></li>
        </ul>
      </nav>
    </header>
  );
}
export default Header;