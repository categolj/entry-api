import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';

const DEFAULT_TENANT = '_';

export function useTenant() {
  const { tenant: tenantParam } = useParams<{ tenant?: string }>();
  const [tenant, setTenant] = useState<string>(tenantParam || DEFAULT_TENANT);

  useEffect(() => {
    setTenant(tenantParam || DEFAULT_TENANT);
  }, [tenantParam]);

  return {
    tenant,
    isDefaultTenant: tenant === DEFAULT_TENANT,
    setTenant,
  };
}