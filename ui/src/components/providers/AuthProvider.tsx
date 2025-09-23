import React, { ReactNode, useEffect } from 'react';
import { AuthContext, useAuthProvider } from '../../hooks/useAuth';
import { setAuthHeaderProvider } from '../../services/api';

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const authValue = useAuthProvider();

  useEffect(() => {
    // Set the auth header provider for the API client
    setAuthHeaderProvider(authValue.getAuthHeader);
  }, [authValue.getAuthHeader]);

  return (
    <AuthContext.Provider value={authValue}>
      {children}
    </AuthContext.Provider>
  );
}