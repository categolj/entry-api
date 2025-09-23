import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { auth } = useAuth();
  const location = useLocation();

  if (!auth.isAuthenticated) {
    // Redirect to login page with the current location as return URL
    return <Navigate to="/" state={{ from: location }} replace />;
  }

  return <>{children}</>;
}