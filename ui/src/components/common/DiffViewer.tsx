import React from 'react';

interface DiffViewerProps {
  originalContent: string;
  newContent: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export function DiffViewer({
  originalContent,
  newContent,
  onConfirm,
  onCancel,
  isLoading = false
}: DiffViewerProps) {
  
  // Improved diff calculation using LCS (Longest Common Subsequence)
  const originalLines = originalContent.split('\n');
  const newLines = newContent.split('\n');
  
  // Calculate LCS to find common lines
  const calculateLCS = (arr1: string[], arr2: string[]) => {
    const m = arr1.length;
    const n = arr2.length;
    const dp: number[][] = Array(m + 1).fill(0).map(() => Array(n + 1).fill(0) as number[]);
    
    for (let i = 1; i <= m; i++) {
      for (let j = 1; j <= n; j++) {
        if (arr1[i - 1] === arr2[j - 1]) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }
    
    // Backtrack to find the diff
    const result = [];
    let i = m, j = n;
    
    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && arr1[i - 1] === arr2[j - 1]) {
        result.unshift({ type: 'unchanged', content: arr1[i - 1], oldLine: i, newLine: j });
        i--;
        j--;
      } else if (j > 0 && (i === 0 || dp[i][j - 1] >= dp[i - 1][j])) {
        result.unshift({ type: 'added', content: arr2[j - 1], oldLine: null, newLine: j });
        j--;
      } else if (i > 0) {
        result.unshift({ type: 'deleted', content: arr1[i - 1], oldLine: i, newLine: null });
        i--;
      }
    }
    
    return result;
  };
  
  const diffLines = calculateLCS(originalLines, newLines);

  return (
    <div className="flex flex-col h-full">
      <div className="mb-4 flex-1 flex flex-col">
        <div className="bg-gray-50 border rounded-lg p-4 flex-1 overflow-y-auto overflow-x-auto">
          <div className="grid grid-cols-12 gap-0 text-xs font-mono min-w-max">
            <div className="col-span-1 text-gray-500 font-bold">Line</div>
            <div className="col-span-11 text-gray-500 font-bold">Content</div>
          </div>
          
          {diffLines.map((line, index) => (
            <div
              key={index}
              className={`grid grid-cols-12 gap-0 text-xs font-mono py-1 min-w-max ${
                line.type === 'added'
                  ? 'bg-green-50 text-green-800'
                  : line.type === 'deleted'
                  ? 'bg-red-50 text-red-800'
                  : 'text-gray-800'
              }`}
            >
              <div className="col-span-1 text-gray-500">
                {line.type === 'deleted' ? line.oldLine : line.type === 'added' ? line.newLine : line.oldLine}
              </div>
              <div className="col-span-11 whitespace-pre-wrap break-words overflow-hidden">
                <span className="inline-block w-4">
                  {line.type === 'added' && <span className="text-green-600">+</span>}
                  {line.type === 'deleted' && <span className="text-red-600">-</span>}
                  {line.type === 'unchanged' && <span className="text-gray-400">&nbsp;</span>}
                </span>
                {line.content || <span className="text-gray-400">(empty line)</span>}
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex justify-end space-x-3 flex-shrink-0 pt-4">
        <button
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 text-sm font-medium text-gray-700 bg-white border border-gray-300 rounded-md hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100 disabled:cursor-not-allowed"
        >
          Back to Edit
        </button>
        <button
          onClick={onConfirm}
          disabled={isLoading}
          className="px-4 py-2 text-sm font-medium text-white bg-blue-600 border border-transparent rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-blue-400 disabled:cursor-not-allowed"
        >
          {isLoading ? 'Saving...' : 'Confirm & Save'}
        </button>
      </div>
    </div>
  );
}