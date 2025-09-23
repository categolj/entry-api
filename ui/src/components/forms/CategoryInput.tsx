import React, { useState } from 'react';

interface CategoryInputProps {
  label?: string;
  value: string[];
  onChange: (categories: string[]) => void;
  placeholder?: string;
  error?: string;
}

export function CategoryInput({ label, value, onChange, placeholder = "Add categories (e.g., Tech::Programming or individual categories)", error }: CategoryInputProps) {
  const [inputValue, setInputValue] = useState('');
  const inputId = `category-input-${Math.random().toString(36).substr(2, 9)}`;

  const addCategory = () => {
    console.log('addCategory called with input:', inputValue);
    const trimmedValue = inputValue.trim();
    if (trimmedValue) {
      // Split by :: to create individual category parts
      const categoryParts = trimmedValue.split('::').map(part => part.trim()).filter(part => part);
      if (categoryParts.length > 0) {
        // Add all category parts (allow duplicates)
        const newCategories = [...value, ...categoryParts];
        console.log('Adding categories:', categoryParts, 'Current categories:', value);
        onChange(newCategories);
        setInputValue('');
      }
    } else {
      console.log('No input value to add');
    }
  };


  const handleButtonClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    console.log('Button clicked');
    addCategory();
  };

  const removeCategory = (indexToRemove: number) => {
    onChange(value.filter((_, index) => index !== indexToRemove));
  };

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-gray-700 mb-1">
          {label}
        </label>
      )}
      
      {/* Existing categories */}
      {value.length > 0 && (
        <div className="mb-3">
          <div className="flex flex-wrap items-center gap-1">
            {value.map((category, index) => (
              <React.Fragment key={index}>
                <span className="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-purple-100 text-purple-800">
                  {category}
                  <button
                    type="button"
                    onClick={() => removeCategory(index)}
                    className="ml-2 text-purple-600 hover:text-purple-800 focus:outline-none"
                    title="Remove category"
                  >
                    ×
                  </button>
                </span>
                {index < value.length - 1 && (
                  <span className="text-purple-600 font-medium text-sm">
                    ::
                  </span>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>
      )}

      {/* Input form */}
      <div className="flex gap-2">
        <input
          id={inputId}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              console.log('Enter key pressed');
              addCategory();
            }
          }}
          placeholder={placeholder}
          className={`flex-1 px-3 py-2 border rounded-lg shadow-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent ${error ? 'border-red-300' : 'border-gray-300'}`}
        />
        <button
          type="button"
          onClick={handleButtonClick}
          disabled={!inputValue.trim()}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-400 disabled:cursor-not-allowed"
        >
          Add
        </button>
      </div>
      
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}