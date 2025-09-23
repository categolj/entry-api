// API response and request types
export interface Entry {
  entryId: number;
  tenantId: string;
  frontMatter: FrontMatter;
  content: string;
  created: AuthorInfo;
  updated: AuthorInfo;
}

export interface FrontMatter {
  title: string;
  categories: Category[];
  tags: Tag[];
  date?: string;
  updated?: string;
  summary?: string;
}

export interface Category {
  name: string;
}

export interface Tag {
  name: string;
  version?: string;
}

export interface TagAndCount {
  name: string;
  version?: string;
  count: number;
}

export interface AuthorInfo {
  name: string;
  date: string;
  url?: string;
  avatarUrl?: string;
}

export interface PaginationResult<T> {
  content: T[];
  size: number;
  hasNext: boolean;
  hasPrevious: boolean;
  cursor?: string | null;
}

export interface SearchCriteria {
  query?: string;
  tag?: string;
  category?: string;
  keyword?: string;
  size?: number;
  cursor?: string;
}

export interface ProblemDetail {
  type: string;
  title: string;
  status: number;
  detail: string;
  instance: string;
}

export interface CreateEntryRequest {
  frontMatter: FrontMatter;
  content: string;
}

export interface UpdateEntryRequest {
  frontMatter?: FrontMatter;
  content?: string;
  summary?: string;
}