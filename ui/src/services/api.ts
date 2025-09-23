import { Entry, PaginationResult, SearchCriteria, CreateEntryRequest, UpdateEntryRequest, TagAndCount, Category, ProblemDetail } from '../types';
import { createMarkdownWithFrontMatter } from '../utils';

const DEFAULT_TENANT = '_';

// Auth context for getting auth header
let getAuthHeader: (() => string | null) | null = null;

export function setAuthHeaderProvider(provider: () => string | null) {
  getAuthHeader = provider;
}

class ApiError extends Error {
  constructor(
    message: string,
    public status: number,
    public problemDetail?: ProblemDetail
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const contentType = response.headers.get('content-type');
    if (contentType?.includes('application/json')) {
      try {
        const problemDetail: ProblemDetail = await response.json();
        throw new ApiError(problemDetail.detail || 'API request failed', response.status, problemDetail);
      } catch (e) {
        if (e instanceof ApiError) throw e;
        throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status);
      }
    } else {
      throw new ApiError(`HTTP ${response.status}: ${response.statusText}`, response.status);
    }
  }

  const contentType = response.headers.get('content-type');
  if (contentType?.includes('application/json')) {
    return response.json();
  } else {
    return response.text() as unknown as T;
  }
}

function buildUrl(tenantId: string, path: string): string {
  const tenant = tenantId || DEFAULT_TENANT;
  return `/tenants/${tenant}${path}`;
}

function buildHeaders(): HeadersInit {
  const headers: HeadersInit = {};
  
  if (getAuthHeader) {
    const authHeader = getAuthHeader();
    if (authHeader) {
      headers['Authorization'] = authHeader;
    }
  }
  
  return headers;
}

export const api = {
  // Entry operations
  async getEntries(tenantId: string, criteria: SearchCriteria = {}): Promise<PaginationResult<Entry>> {
    const params = new URLSearchParams();
    if (criteria.query) params.append('query', criteria.query);
    if (criteria.tag) params.append('tag', criteria.tag);
    if (criteria.category) params.append('category', criteria.category);
    if (criteria.keyword) params.append('keyword', criteria.keyword);
    if (criteria.size) params.append('size', criteria.size.toString());
    if (criteria.cursor) params.append('cursor', criteria.cursor);

    const url = buildUrl(tenantId, '/entries') + (params.toString() ? `?${params}` : '');
    console.log('API getEntries - URL:', url, 'Criteria:', criteria);
    const response = await fetch(url, {
      headers: buildHeaders(),
    });
    return handleResponse<PaginationResult<Entry>>(response);
  },

  async getEntry(tenantId: string, entryId: number): Promise<Entry> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      headers: buildHeaders(),
    });
    return handleResponse<Entry>(response);
  },

  async createEntry(tenantId: string, request: CreateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter, request.content);
    
    const response = await fetch(buildUrl(tenantId, '/entries'), {
      method: 'POST',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async createEntryWithId(tenantId: string, entryId: number, request: CreateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter, request.content);
    
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async updateEntry(tenantId: string, entryId: number, request: UpdateEntryRequest): Promise<Entry> {
    // Convert to markdown format for API
    const markdownContent = createMarkdownWithFrontMatter(request.frontMatter!, request.content!);
    
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/markdown',
        ...buildHeaders(),
      },
      body: markdownContent,
    });
    return handleResponse<Entry>(response);
  },

  async updateEntrySummary(tenantId: string, entryId: number, summary: string): Promise<Entry> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}/summary`), {
      method: 'PUT',
      headers: {
        'Content-Type': 'text/plain',
        ...buildHeaders(),
      },
      body: summary,
    });
    return handleResponse<Entry>(response);
  },

  async deleteEntry(tenantId: string, entryId: number): Promise<void> {
    const response = await fetch(buildUrl(tenantId, `/entries/${entryId}`), {
      method: 'DELETE',
      headers: buildHeaders(),
    });
    if (!response.ok) {
      await handleResponse(response);
    }
  },

  async searchEntries(tenantId: string, query: string): Promise<PaginationResult<Entry>> {
    const response = await fetch(buildUrl(tenantId, `/entries/search?query=${encodeURIComponent(query)}`), {
      headers: buildHeaders(),
    });
    return handleResponse<PaginationResult<Entry>>(response);
  },

  // Category operations
  async getCategories(tenantId: string): Promise<Category[][]> {
    const response = await fetch(buildUrl(tenantId, '/categories'), {
      headers: buildHeaders(),
    });
    return handleResponse<Category[][]>(response);
  },

  // Tag operations
  async getTags(tenantId: string): Promise<TagAndCount[]> {
    const response = await fetch(buildUrl(tenantId, '/tags'), {
      headers: buildHeaders(),
    });
    return handleResponse<TagAndCount[]>(response);
  },
};

export { ApiError };