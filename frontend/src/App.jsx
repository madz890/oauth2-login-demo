import React from 'react';
import Home from './components/Home';
import Profile from './components/Profile';

export default function App() {
  const path = window.location.pathname;
  if (path === '/profile') {
    return <Profile />;
  }
  return <Home />;
}
