import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation, Link } from 'react-router-dom';
import { useTenant, useApi } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button, DiffViewer } from '../../components/common';
import { FrontMatter, CreateEntryRequest, UpdateEntryRequest } from '../../types';
import { createMarkdownWithFrontMatter } from '../../utils';

interface PreviewState {
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
  existingEntry?: any;
  originalMarkdown?: string;
}

export function EntryPreview() {
  const { tenant } = useTenant();
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state as PreviewState;
  
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [existingEntryForId, setExistingEntryForId] = useState<any>(null);
  const [originalMarkdown, setOriginalMarkdown] = useState('');

  // Redirect back if no state
  useEffect(() => {
    if (!state) {
      navigate(`/console/${tenant}/entries/new`);
      return;
    }

    // For create mode with entry ID, check if entry exists
    if (state.mode === 'create' && state.entryIdInput?.trim()) {
      const checkExistingEntry = async () => {
        try {
          const entryId = parseInt(state.entryIdInput!.trim(), 10);
          const existing = await api.getEntry(tenant, entryId);
          setExistingEntryForId(existing);
          
          const existingMarkdown = createMarkdownWithFrontMatter(existing.frontMatter, existing.content);
          setOriginalMarkdown(existingMarkdown);
        } catch (error) {
          // Entry doesn't exist, which is fine for create with ID
          setExistingEntryForId(null);
          setOriginalMarkdown('');
        }
      };
      
      checkExistingEntry();
    } else if (state.mode === 'edit') {
      setOriginalMarkdown(state.originalMarkdown || '');
    }
  }, [state, tenant, navigate]);

  const handleConfirmSubmit = async () => {
    if (!state) return;
    
    setIsSubmitting(true);
    setSubmitError(null);

    try {
      const frontMatter: FrontMatter = {
        title: state.formData.title,
        summary: state.formData.summary || undefined,
        categories: state.formData.categories.map(name => ({ name })),
        tags: state.formData.tags.map(name => ({ name })),
        // Include date/updated logic here based on mode and updateTimestamp
        ...(state.existingEntry?.frontMatter.date && { date: state.existingEntry.frontMatter.date }),
        ...(state.mode === 'edit' && !state.updateTimestamp && (state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date) && { 
          updated: state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date 
        }),
      };

      if (state.mode === 'create') {
        const request: CreateEntryRequest = {
          frontMatter,
          content: state.formData.content,
        };
        
        let newEntry;
        if (state.entryIdInput?.trim()) {
          const entryId = parseInt(state.entryIdInput.trim(), 10);
          newEntry = await api.createEntryWithId(tenant, entryId, request);
        } else {
          newEntry = await api.createEntry(tenant, request);
        }
        navigate(`/console/${tenant}/entries/${newEntry.entryId}`);
      } else {
        const request: UpdateEntryRequest = {
          frontMatter,
          content: state.formData.content,
        };
        
        // Extract entry ID from the edit path: /console/{tenant}/entries/{id}/edit/preview
        const pathParts = location.pathname.split('/');
        const idIndex = pathParts.indexOf('entries') + 1;
        const entryId = parseInt(pathParts[idIndex], 10);
        const updatedEntry = await api.updateEntry(tenant, entryId, request);
        navigate(`/console/${tenant}/entries/${updatedEntry.entryId}`);
      }
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : 'Failed to save entry');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleBack = () => {
    // For edit mode, extract the numeric ID from the path
    // Path format: /console/{tenant}/entries/{id}/edit/preview
    const pathParts = location.pathname.split('/');
    const idIndex = pathParts.indexOf('entries') + 1;
    const entryId = state.mode === 'edit' ? pathParts[idIndex] : null;
    
    const backPath = state.mode === 'edit' 
      ? `/console/${tenant}/entries/${entryId}/edit`
      : `/console/${tenant}/entries/new`;
    
    navigate(backPath, { state });
  };

  const getCurrentMarkdown = () => {
    if (!state) return '';
    
    const frontMatter: FrontMatter = {
      title: state.formData.title,
      summary: state.formData.summary || undefined,
      categories: state.formData.categories.map(name => ({ name })),
      tags: state.formData.tags.map(name => ({ name })),
      ...(state.existingEntry?.frontMatter.date && { date: state.existingEntry.frontMatter.date }),
      ...(state.mode === 'edit' && !state.updateTimestamp && (state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date) && { 
        updated: state.existingEntry?.frontMatter.updated || state.existingEntry?.updated.date 
      }),
    };
    return createMarkdownWithFrontMatter(frontMatter, state.formData.content);
  };

  if (!state) {
    return null;
  }

  const title = state.mode === 'create' 
    ? (existingEntryForId ? `Review Changes for Entry ${state.entryIdInput}` : 'Review New Entry')
    : 'Review Changes';

  return (
    <div className="px-4 py-3 sm:px-0">
      {/* Header */}
      <div className="mb-4">
        <nav className="flex" aria-label="Breadcrumb">
          <ol className="flex items-center space-x-3">
            <li>
              <Link to={`/console/${tenant}`} className="text-gray-400 hover:text-gray-500 text-sm">
                Entries
              </Link>
            </li>
            <li>
              <span className="text-gray-400">/</span>
            </li>
            <li>
              <button
                onClick={handleBack}
                className="text-gray-400 hover:text-gray-500 text-sm"
              >
                {state.mode === 'create' ? 'Create New Entry' : 'Edit Entry'}
              </button>
            </li>
            <li>
              <span className="text-gray-400">/</span>
            </li>
            <li>
              <span className="text-gray-500 text-sm">Preview</span>
            </li>
          </ol>
        </nav>
        <h1 className="mt-1 text-xl font-bold text-gray-900">{title}</h1>
      </div>

      {/* Error Display */}
      {submitError && (
        <div className="mb-4">
          <ErrorAlert message={submitError} onDismiss={() => setSubmitError(null)} />
        </div>
      )}

      {/* Diff Viewer */}
      <div className="bg-white shadow rounded-lg p-4 h-96">
        <DiffViewer
          originalContent={originalMarkdown}
          newContent={getCurrentMarkdown()}
          onConfirm={handleConfirmSubmit}
          onCancel={handleBack}
          isLoading={isSubmitting}
        />
      </div>
    </div>
  );
}