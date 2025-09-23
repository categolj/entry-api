import { Entry } from './api';

export interface PreviewState {
  mode: 'create' | 'edit';
  formData: {
    title: string;
    summary: string;
    categories: string[];
    tags: string[];
    content: string;
  };
  entryIdInput?: string;
  updateTimestamp?: boolean;
  existingEntry?: Entry;
  originalMarkdown?: string;
}

export interface LocationState {
  from?: {
    pathname: string;
  };
}