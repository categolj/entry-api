import { FrontMatter } from '../types';

export function parseMarkdownWithFrontMatter(content: string): { frontMatter: Partial<FrontMatter>; content: string } {
  const frontMatterRegex = /^---\n([\s\S]*?)\n---\n([\s\S]*)$/;
  const match = content.match(frontMatterRegex);

  if (!match) {
    return {
      frontMatter: {},
      content: content.trim(),
    };
  }

  const frontMatterText = match[1];
  const markdownContent = match[2].trim();

  // Simple YAML parser for basic fields
  const frontMatter: Partial<FrontMatter> = {};
  
  frontMatterText.split('\n').forEach(line => {
    const colonIndex = line.indexOf(':');
    if (colonIndex === -1) return;
    
    const key = line.substring(0, colonIndex).trim();
    const value = line.substring(colonIndex + 1).trim();
    
    switch (key) {
      case 'title':
        frontMatter.title = value.replace(/^["']|["']$/g, ''); // Remove quotes
        break;
      case 'summary':
        frontMatter.summary = value.replace(/^["']|["']$/g, '');
        break;
      case 'categories':
        if (value.startsWith('[') && value.endsWith(']')) {
          const categoriesText = value.slice(1, -1);
          frontMatter.categories = categoriesText
            .split(',')
            .map(cat => ({ name: cat.trim().replace(/^["']|["']$/g, '') }))
            .filter(cat => cat.name);
        }
        break;
      case 'tags':
        if (value.startsWith('[') && value.endsWith(']')) {
          const tagsText = value.slice(1, -1);
          frontMatter.tags = tagsText
            .split(',')
            .map(tag => ({ name: tag.trim().replace(/^["']|["']$/g, '') }))
            .filter(tag => tag.name);
        }
        break;
      case 'date':
        frontMatter.date = value.replace(/^["']|["']$/g, '');
        break;
      case 'updated':
        frontMatter.updated = value.replace(/^["']|["']$/g, '');
        break;
    }
  });

  return {
    frontMatter,
    content: markdownContent,
  };
}

export function createMarkdownWithFrontMatter(frontMatter: FrontMatter, content: string): string {
  const lines = ['---'];
  
  if (frontMatter.title) {
    lines.push(`title: "${frontMatter.title}"`);
  }
  
  if (frontMatter.summary) {
    lines.push(`summary: "${frontMatter.summary}"`);
  }
  
  if (frontMatter.categories && frontMatter.categories.length > 0) {
    const categoriesString = frontMatter.categories.map(cat => `"${cat.name}"`).join(', ');
    lines.push(`categories: [${categoriesString}]`);
  }
  
  if (frontMatter.tags && frontMatter.tags.length > 0) {
    const tagsString = frontMatter.tags.map(tag => `"${tag.name}"`).join(', ');
    lines.push(`tags: [${tagsString}]`);
  }
  
  if (frontMatter.date) {
    lines.push(`date: ${frontMatter.date}`);
  }
  
  if (frontMatter.updated) {
    lines.push(`updated: ${frontMatter.updated}`);
  }
  
  lines.push('---');
  lines.push('');
  lines.push(content);
  
  return lines.join('\n');
}