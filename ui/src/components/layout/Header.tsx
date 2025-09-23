import React from 'react';
import { Link } from 'react-router-dom';

interface HeaderProps {
  tenant?: string;
  isDefaultTenant?: boolean;
  showTenantInfo?: boolean;
  showNavigation?: boolean;
}

export function Header({ tenant, isDefaultTenant, showTenantInfo = false, showNavigation = false }: HeaderProps) {
  return (
    <nav className="bg-white shadow">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16">
          <div className="flex">
            <div className="flex-shrink-0 flex items-center">
              <Link
                to="/"
                className="text-xl font-bold text-gray-900 hover:text-gray-700 transition-colors"
              >
                Entry API Admin
              </Link>
            </div>
            {showNavigation && tenant && (
              <div className="hidden sm:ml-6 sm:flex sm:space-x-8">
                <Link
                  to={`/console/${tenant}`}
                  className="border-blue-500 text-gray-900 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium"
                >
                  Entries
                </Link>
              </div>
            )}
          </div>
          {showTenantInfo && tenant && (
            <div className="flex items-center">
              <div className="flex-shrink-0">
                <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-medium bg-blue-100 text-blue-800">
                  Tenant: {isDefaultTenant ? 'Default' : tenant}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}