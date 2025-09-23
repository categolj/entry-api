import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useTenant, useApi } from '../../hooks';
import { api } from '../../services';
import { LoadingSpinner, ErrorAlert, Button } from '../../components/common';
import { Input } from '../../components/forms';
import { Entry, SearchCriteria } from '../../types';

export function EntryList() {
  const { tenant } = useTenant();
  const [searchCriteria, setSearchCriteria] = useState<SearchCriteria>({});
  const [searchQuery, setSearchQuery] = useState('');
  const [isInitialLoad, setIsInitialLoad] = useState(true);
  const [isSearching, setIsSearching] = useState(false);

  const {
    data: entriesResult,
    loading,
    error,
    execute,
  } = useApi(
    () => api.getEntries(tenant, searchCriteria),
    [tenant, searchCriteria]
  );

  React.useEffect(() => {
    if (loading && isInitialLoad) {
      setIsInitialLoad(false);
    }
  }, [loading, isInitialLoad]);

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    const newCriteria = { query: searchQuery.trim() || undefined };
    console.log('Search submitted with criteria:', newCriteria);
    setIsSearching(true);
    setSearchCriteria(newCriteria);
  };

  React.useEffect(() => {
    if (isSearching && !loading) {
      setIsSearching(false);
    }
  }, [loading, isSearching]);

  const handleClearFilters = () => {
    setSearchQuery('');
    setSearchCriteria({});
  };


  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatCategories = (categories: { name: string }[]) => {
    return categories.map(c => c.name).join(', ');
  };

  const formatTags = (tags: { name: string }[]) => {
    return tags.map(t => t.name).join(', ');
  };

  // Only show full screen loading on initial load
  if (loading && isInitialLoad) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div className="px-4 py-3 sm:px-0">
      {/* Header */}
      <div className="sm:flex sm:items-center sm:justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Entries</h1>
          <p className="mt-1 text-sm text-gray-700">
            Manage blog entries for tenant: {tenant}
          </p>
        </div>
        <div className="mt-3 sm:mt-0">
          <Link to={`/console/${tenant}/entries/new`}>
            <Button>Create New Entry</Button>
          </Link>
        </div>
      </div>

      {/* Search and Filters */}
      <div className="mt-4 bg-white shadow rounded-lg p-4">
        <form onSubmit={handleSearch} className="space-y-4 sm:space-y-0 sm:flex sm:items-end sm:space-x-4">
          <div className="flex-1">
            <Input
              label="Search"
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search entries..."
              helpText="Search across title, content, categories, and tags"
            />
          </div>
          <div className="flex space-x-2">
            <Button type="submit" disabled={loading && isSearching}>
              {loading && isSearching ? 'Searching...' : 'Search'}
            </Button>
            <Button type="button" variant="secondary" onClick={handleClearFilters} disabled={loading && isSearching}>
              Clear
            </Button>
          </div>
        </form>
      </div>

      {/* Error Display */}
      {error && (
        <div className="mt-4">
          <ErrorAlert message={error} onDismiss={() => execute()} />
        </div>
      )}

      {/* Results */}
      <div className="mt-4">
        <div className="bg-white shadow overflow-hidden sm:rounded-md">
          <div className="px-4 py-3 border-b border-gray-200 sm:px-6">
            <h3 className="text-lg leading-6 font-medium text-gray-900">
              {loading && isSearching ? (
                <span className="flex items-center">
                  <span className="mr-2"><LoadingSpinner size="sm" /></span>
                  Searching...
                </span>
              ) : (
                entriesResult ? `${entriesResult.content.length} entries found` : 'Loading...'
              )}
            </h3>
          </div>
          
          {loading && isSearching ? (
            <div className="px-4 py-8 text-center">
              <LoadingSpinner size="md" />
              <p className="mt-2 text-gray-500">Searching entries...</p>
            </div>
          ) : !entriesResult ? (
            <div className="px-4 py-8 text-center">
              <p className="text-gray-500">Loading entries...</p>
            </div>
          ) : entriesResult.content.length === 0 ? (
              <div className="px-4 py-8 text-center">
                <p className="text-gray-500">No entries found.</p>
                <Link to={`/console/${tenant}/entries/new`} className="mt-2 inline-block">
                  <Button>Create your first entry</Button>
                </Link>
              </div>
            ) : (
              <ul className="divide-y divide-gray-200">
                {entriesResult.content.map((entry: Entry) => (
                  <li key={entry.entryId}>
                    <Link
                      to={`/console/${tenant}/entries/${entry.entryId}`}
                      className="block hover:bg-gray-50 px-4 py-3 sm:px-6"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex-1">
                          <div className="flex items-center justify-between">
                            <p className="text-lg font-medium text-blue-600 truncate">
                              {entry.frontMatter.title}
                            </p>
                            <div className="ml-2 flex-shrink-0 flex">
                              <p className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                ID: {entry.entryId}
                              </p>
                            </div>
                          </div>
                          {entry.frontMatter.summary && (
                            <p className="mt-2 text-sm text-gray-600">
                              {entry.frontMatter.summary}
                            </p>
                          )}
                          <div className="mt-2 sm:flex sm:justify-between">
                            <div className="sm:flex">
                              {entry.frontMatter.categories.length > 0 && (
                                <p className="flex items-center text-sm text-gray-500">
                                  <span className="font-medium">Categories:</span>
                                  <span className="ml-1">{formatCategories(entry.frontMatter.categories)}</span>
                                </p>
                              )}
                              {entry.frontMatter.tags.length > 0 && (
                                <p className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0 sm:ml-6">
                                  <span className="font-medium">Tags:</span>
                                  <span className="ml-1">{formatTags(entry.frontMatter.tags)}</span>
                                </p>
                              )}
                            </div>
                            <div className="mt-2 flex items-center text-sm text-gray-500 sm:mt-0">
                              <p>
                                Updated {formatDate(entry.updated.date)} by {entry.updated.name}
                              </p>
                            </div>
                          </div>
                        </div>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}

          {/* Pagination */}
          {entriesResult && (entriesResult.hasNext || entriesResult.hasPrevious) && (
            <div className="mt-6 flex justify-between">
              <div>
                {entriesResult.hasPrevious && (
                  <Button
                    variant="secondary"
                    onClick={() => {
                      // TODO: Implement pagination
                    }}
                  >
                    Previous
                  </Button>
                )}
              </div>
              <div>
                {entriesResult.hasNext && (
                  <Button
                    onClick={() => {
                      // TODO: Implement pagination
                    }}
                  >
                    Next
                  </Button>
                )}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}