import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Header } from '../../components/layout';
import { useAuth } from '../../hooks';

const DEFAULT_TENANT = '_';

export function TenantSelector() {
  const [customTenant, setCustomTenant] = useState('');
  const { auth, login } = useAuth();
  const [username, setUsername] = useState(auth.username);
  const [password, setPassword] = useState(auth.password);
  const [authError, setAuthError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  // Update form fields when auth state changes
  useEffect(() => {
    setUsername(auth.username);
    setPassword(auth.password);
  }, [auth.username, auth.password]);

  // If already authenticated and coming from a protected route, redirect back
  useEffect(() => {
    if (auth.isAuthenticated && location.state?.from) {
      navigate(location.state.from.pathname, { replace: true });
    }
  }, [auth.isAuthenticated, location.state, navigate]);

  const handleTenantAccess = (tenantId: string) => {
    if (!username.trim() || !password.trim()) {
      setAuthError('Please enter both username and password');
      return;
    }

    login(username, password);
    navigate(`/console/${tenantId}`);
  };

  const handleDefaultTenant = () => {
    handleTenantAccess(DEFAULT_TENANT);
  };

  const handleCustomTenantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (customTenant.trim()) {
      handleTenantAccess(customTenant.trim());
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />
      
      <div className="flex items-center justify-center py-8 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-4">
          <div>
            <h2 className="text-center text-2xl font-extrabold text-gray-900">
              Select Tenant
            </h2>
            <p className="mt-1 text-center text-sm text-gray-600">
              Choose a tenant to manage entries
            </p>
          </div>

          {/* Authentication Section */}
          <div className="bg-white shadow rounded-lg p-4">
            <h3 className="text-lg font-medium text-gray-900 mb-3">Authentication</h3>
            <div className="space-y-3">
              <div>
                <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-1">
                  Username
                </label>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    setAuthError('');
                  }}
                  placeholder="Enter username"
                  className="appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <div>
                <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-1">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setAuthError('');
                  }}
                  placeholder="Enter password"
                  className="appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              {authError && (
                <p className="text-sm text-red-600">{authError}</p>
              )}
            </div>
          </div>

        <div className="mt-6 space-y-4">
          {/* Default Tenant */}
          <div className="space-y-3">
            <button
              onClick={handleDefaultTenant}
              disabled={!username.trim() || !password.trim()}
              className="w-full text-left p-4 border border-gray-300 rounded-lg hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent transition-colors disabled:bg-gray-100 disabled:cursor-not-allowed disabled:text-gray-400"
            >
              <div className="font-medium text-gray-900">Default Tenant</div>
              <div className="text-sm text-gray-500 mt-1">Use the default tenant for general entries</div>
              <div className="text-xs text-gray-400 mt-1">Tenant ID: {DEFAULT_TENANT}</div>
            </button>
          </div>

          {/* OR Divider */}
          <div className="relative">
            <div className="absolute inset-0 flex items-center">
              <div className="w-full border-t border-gray-300" />
            </div>
            <div className="relative flex justify-center text-sm">
              <span className="px-2 bg-gray-50 text-gray-500">OR</span>
            </div>
          </div>

          {/* Custom Tenant */}
          <div className="space-y-3">
            <h3 className="text-base font-medium text-gray-900">Custom Tenant</h3>
            <form onSubmit={handleCustomTenantSubmit} className="space-y-3">
              <div>
                <label htmlFor="custom-tenant" className="block text-sm font-medium text-gray-700 mb-1">
                  Tenant ID
                </label>
                <input
                  id="custom-tenant"
                  type="text"
                  value={customTenant}
                  onChange={(e) => setCustomTenant(e.target.value)}
                  placeholder="Enter tenant ID (e.g., blog, docs, etc.)"
                  className="appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>
              <button
                type="submit"
                disabled={!customTenant.trim() || !username.trim() || !password.trim()}
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-lg shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed transition-colors"
              >
                Access Custom Tenant
              </button>
            </form>
          </div>
          </div>
        </div>
      </div>
    </div>
  );
}