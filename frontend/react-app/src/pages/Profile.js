import React from 'react';
import { useSelector } from 'react-redux';

const Profile = () => {
  const { user } = useSelector((state) => state.auth);

  if (!user) return <div>Chargement...</div>;

  return (
    <div className="profile">
      <h1>Mon Profil</h1>
      <div className="profile-card">
        <h2>Informations personnelles</h2>
        <p><strong>Nom d'utilisateur:</strong> {user.username}</p>
        <p><strong>Email:</strong> {user.email}</p>
        <p><strong>Nom complet:</strong> {user.full_name}</p>
        <p><strong>ID:</strong> {user.id}</p>
        <p><strong>Membre depuis:</strong> {new Date(user.created_at).toLocaleDateString()}</p>
      </div>
    </div>
  );
};

export default Profile;
