// src/App.tsx
import React from 'react';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import Header from './components/Header.tsx';
import Login from './components/Login.tsx';
import PlayerList from './components/PlayerList.tsx';
import Register from './components/Register.tsx';

function App() {
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={<PlayerList />} /> {}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        {}
      </Routes>
    </Router>
  );
}

export default App;
