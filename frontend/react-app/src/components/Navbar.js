import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useDispatch } from 'react-redux';
import { logout } from '../store/slices/authSlice';

const Navbar = () => {
  const dispatch = useDispatch();
  const navigate = useNavigate();

  const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="nav-brand">
        <Link to="/dashboard">E-commerce</Link>
      </div>
      <ul className="nav-links">
        <li><Link to="/dashboard">Tableau de bord</Link></li>
        <li><Link to="/products">Produits</Link></li>
        <li><Link to="/orders">Commandes</Link></li>
        <li><Link to="/profile">Profil</Link></li>
        <li><button onClick={handleLogout}>Déconnexion</button></li>
      </ul>
    </nav>
  );
};

export default Navbar;
