import { useState, createContext, useContext, ReactNode } from 'react';

interface AuthState {
  username: string;
  password: string;
  isAuthenticated: boolean;
}

interface AuthContextType {
  auth: AuthState;
  login: (username: string, password: string) => void;
  logout: () => void;
  getAuthHeader: () => string | null;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function useAuth() {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}

export function useAuthProvider(): AuthContextType {
  const [auth, setAuth] = useState<AuthState>({
    username: '',
    password: '',
    isAuthenticated: false,
  });

  const login = (username: string, password: string) => {
    setAuth({
      username,
      password,
      isAuthenticated: true,
    });
  };

  const logout = () => {
    setAuth({
      username: '',
      password: '',
      isAuthenticated: false,
    });
  };

  const getAuthHeader = (): string | null => {
    if (!auth.isAuthenticated || !auth.username || !auth.password) {
      return null;
    }
    
    const credentials = btoa(`${auth.username}:${auth.password}`);
    return `Basic ${credentials}`;
  };

  return {
    auth,
    login,
    logout,
    getAuthHeader,
  };
}

export { AuthContext };