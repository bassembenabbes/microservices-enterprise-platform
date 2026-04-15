import React from 'react';
import { useSelector } from 'react-redux';

const Dashboard = () => {
  const { user } = useSelector((state) => state.auth);

  return (
    <div className="dashboard">
      <h1>Tableau de bord</h1>
      <div className="welcome-card">
        <h2>Bonjour, {user?.username || 'Utilisateur'}!</h2>
        <p>Bienvenue dans votre espace e-commerce.</p>
      </div>
      <div className="dashboard-grid">
        <div className="card">
          <h3>Produits</h3>
          <p>Parcourez notre catalogue</p>
          <a href="/products">Voir les produits</a>
        </div>
        <div className="card">
          <h3>Commandes</h3>
          <p>Gérez vos commandes</p>
          <a href="/orders">Voir les commandes</a>
        </div>
        <div className="card">
          <h3>Profil</h3>
          <p>Modifiez vos informations</p>
          <a href="/profile">Voir le profil</a>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
