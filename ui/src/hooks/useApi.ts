import { useState, useEffect } from 'react';
import { ApiError } from '../services';

interface UseApiState<T> {
  data: T | null;
  loading: boolean;
  error: string | null;
}

interface UseApiOptions {
  immediate?: boolean;
}

export function useApi<T>(
  apiCall: () => Promise<T>,
  dependencies: React.DependencyList = [],
  options: UseApiOptions = { immediate: true }
) {
  const [state, setState] = useState<UseApiState<T>>({
    data: null,
    loading: options.immediate ?? true,
    error: null,
  });

  const execute = async () => {
    setState(prev => ({ ...prev, loading: true, error: null }));
    
    try {
      const data = await apiCall();
      setState({ data, loading: false, error: null });
      return data;
    } catch (error) {
      const errorMessage = error instanceof ApiError 
        ? error.message 
        : error instanceof Error 
        ? error.message 
        : 'An unexpected error occurred';
      
      setState({ data: null, loading: false, error: errorMessage });
      throw error;
    }
  };

  useEffect(() => {
    if (options.immediate !== false) {
      void execute();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);

  return {
    ...state,
    execute,
    refetch: execute,
  };
}