import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { TenantSelector } from './pages/tenant';
import { EntryList, EntryDetail, CreateEntry, EditEntry, EntryPreview } from './pages/entries';
import { Layout } from './components/layout';
import { AuthProvider } from './components/providers/AuthProvider';
import { ProtectedRoute } from './components/auth';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          {/* Root route - tenant selection */}
          <Route path="/" element={<TenantSelector />} />
          
          {/* Tenant-specific routes */}
          <Route path="/console/:tenant" element={<ProtectedRoute><Layout /></ProtectedRoute>}>
            <Route index element={<EntryList />} />
            <Route path="entries/new" element={<CreateEntry />} />
            <Route path="entries/new/preview" element={<EntryPreview />} />
            <Route path="entries/:id" element={<EntryDetail />} />
            <Route path="entries/:id/edit" element={<EditEntry />} />
            <Route path="entries/:id/edit/preview" element={<EntryPreview />} />
          </Route>
          
          {/* Catch all route */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
