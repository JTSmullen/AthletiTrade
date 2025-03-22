// src/App.tsx
import React from 'react';
import { Route, BrowserRouter as Router, Routes } from 'react-router-dom';
import Header from './components/Header';
import Login from './components/Login';
import PlayerList from './components/PlayerList';
import Register from './components/Register';

function App() {
  return (
    <Router>
      <Header />
      <Routes>
        <Route path="/" element={<PlayerList />} /> {/* Default route */}
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        {/* Add more routes here later (e.g., /players/:id, /portfolio) */}
      </Routes>
    </Router>
  );
}

export default App;
