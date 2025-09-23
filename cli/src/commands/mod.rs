pub mod category;
pub mod config;
pub mod entry;
pub mod tag;

use crate::cli::OutputFormat;
use crate::client::models::{Entry, TagAndCount};
use colored::Colorize;
use tabled::{Table, Tabled};

pub fn format_output<T: serde::Serialize>(data: &T, format: &OutputFormat) -> String {
    match format {
        OutputFormat::Json => serde_json::to_string_pretty(data).unwrap_or_default(),
        _ => serde_json::to_string_pretty(data).unwrap_or_default(),
    }
}

#[derive(Tabled)]
struct EntryRow {
    #[tabled(rename = "ID")]
    id: String,
    #[tabled(rename = "Title")]
    title: String,
    #[tabled(rename = "Categories")]
    categories: String,
    #[tabled(rename = "Tags")]
    tags: String,
    #[tabled(rename = "Updated")]
    updated: String,
}

pub fn format_entries_table(entries: &[Entry]) -> String {
    let rows: Vec<EntryRow> = entries
        .iter()
        .map(|e| EntryRow {
            id: e.entry_id.to_string(),
            title: e.front_matter.title.clone(),
            categories: e
                .front_matter
                .categories
                .iter()
                .map(|c| c.name.as_str())
                .collect::<Vec<_>>()
                .join("::"),
            tags: e
                .front_matter
                .tags
                .iter()
                .map(|t| t.name.as_str())
                .collect::<Vec<_>>()
                .join(", "),
            updated: e
                .updated
                .date
                .map(|d| d.format("%Y-%m-%d %H:%M").to_string())
                .unwrap_or_default(),
        })
        .collect();

    Table::new(rows).to_string()
}

#[derive(Tabled)]
struct TagRow {
    #[tabled(rename = "Tag")]
    name: String,
    #[tabled(rename = "Count")]
    count: u32,
}

pub fn format_tags_table(tags: &[TagAndCount]) -> String {
    let rows: Vec<TagRow> = tags
        .iter()
        .map(|t| TagRow {
            name: t.name.clone(),
            count: t.count,
        })
        .collect();

    Table::new(rows).to_string()
}

pub fn print_success(message: &str) {
    println!("{} {}", "✓".green().bold(), message);
}

pub fn print_error(message: &str) {
    eprintln!("{} {}", "✗".red().bold(), message);
}

pub fn print_warning(message: &str) {
    eprintln!("{} {}", "!".yellow().bold(), message);
}

pub fn print_info(message: &str) {
    println!("{} {}", "ℹ".blue().bold(), message);
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::client::models::{Author, Category, FrontMatter, Tag};
    use chrono::{DateTime, Utc};
    use serde_json;

    fn create_test_entry(id: u64, title: &str) -> Entry {
        Entry {
            entry_id: id,
            tenant_id: "_".to_string(),
            front_matter: FrontMatter {
                title: title.to_string(),
                summary: "Test summary".to_string(),
                categories: vec![
                    Category { name: "tech".to_string() },
                    Category { name: "programming".to_string() },
                ],
                tags: vec![
                    Tag { name: "rust".to_string(), version: None },
                    Tag { name: "cli".to_string(), version: None },
                ],
            },
            content: Some("# Test Content".to_string()),
            created: Author {
                name: "author".to_string(),
                date: Some(DateTime::parse_from_rfc3339("2023-01-01T10:00:00Z").unwrap().with_timezone(&Utc)),
            },
            updated: Author {
                name: "author".to_string(),
                date: Some(DateTime::parse_from_rfc3339("2023-01-02T15:30:00Z").unwrap().with_timezone(&Utc)),
            },
        }
    }

    fn create_test_tag(name: &str, count: u32) -> TagAndCount {
        TagAndCount {
            name: name.to_string(),
            version: Some("1.0".to_string()),
            count,
        }
    }

    #[test]
    fn test_format_output_json() {
        let data = serde_json::json!({
            "test": "value",
            "number": 42
        });

        let result = format_output(&data, &OutputFormat::Json);
        assert!(result.contains("\"test\": \"value\""));
        assert!(result.contains("\"number\": 42"));
    }

    #[test]
    fn test_format_entries_table() {
        let entries = vec![
            create_test_entry(1, "First Entry"),
            create_test_entry(2, "Second Entry"),
        ];

        let table = format_entries_table(&entries);
        
        // Check table contains entry data
        assert!(table.contains("First Entry"));
        assert!(table.contains("Second Entry"));
        assert!(table.contains("tech::programming")); // Categories with :: separator
        assert!(table.contains("rust, cli")); // Tags with comma separator
        assert!(table.contains("2023-01-02 15:30")); // Updated date
        
        // Check table headers
        assert!(table.contains("ID"));
        assert!(table.contains("Title"));
        assert!(table.contains("Categories"));
        assert!(table.contains("Tags"));
        assert!(table.contains("Updated"));
    }

    #[test]
    fn test_format_entries_table_empty() {
        let entries: Vec<Entry> = vec![];
        let table = format_entries_table(&entries);
        
        // Should still have headers but no data rows
        assert!(table.contains("ID"));
        assert!(table.contains("Title"));
    }

    #[test]
    fn test_format_tags_table() {
        let tags = vec![
            create_test_tag("rust", 10),
            create_test_tag("cli", 5),
            create_test_tag("testing", 3),
        ];

        let table = format_tags_table(&tags);
        
        // Check table contains tag data
        assert!(table.contains("rust"));
        assert!(table.contains("cli"));
        assert!(table.contains("testing"));
        assert!(table.contains("10"));
        assert!(table.contains("5"));
        assert!(table.contains("3"));
        
        // Check table headers (should not contain Version)
        assert!(table.contains("Tag"));
        assert!(table.contains("Count"));
        assert!(!table.contains("Version")); // Version column should be removed
    }

    #[test]
    fn test_format_tags_table_empty() {
        let tags: Vec<TagAndCount> = vec![];
        let table = format_tags_table(&tags);
        
        // Should still have headers but no data rows
        assert!(table.contains("Tag"));
        assert!(table.contains("Count"));
        assert!(!table.contains("Version"));
    }

    #[test]
    fn test_entry_with_empty_categories_and_tags() {
        let mut entry = create_test_entry(1, "Test Entry");
        entry.front_matter.categories = vec![];
        entry.front_matter.tags = vec![];

        let table = format_entries_table(&vec![entry]);
        
        // Should handle empty categories and tags gracefully
        assert!(table.contains("Test Entry"));
        // Categories and tags columns should be empty but present
        assert!(table.contains("Categories"));
        assert!(table.contains("Tags"));
    }

    #[test]
    fn test_entry_with_no_updated_date() {
        let mut entry = create_test_entry(1, "Test Entry");
        entry.updated.date = None;

        let table = format_entries_table(&vec![entry]);
        
        // Should handle missing updated date gracefully
        assert!(table.contains("Test Entry"));
        assert!(table.contains("Updated"));
    }

    #[test]
    fn test_category_separator() {
        let entry = create_test_entry(1, "Test Entry");
        let table = format_entries_table(&vec![entry]);
        
        // Categories should be separated by "::" without spaces
        assert!(table.contains("tech::programming"));
        assert!(!table.contains("tech > programming"));
        assert!(!table.contains("tech :: programming"));
    }

    #[test]
    fn test_tag_separator() {
        let entry = create_test_entry(1, "Test Entry");
        let table = format_entries_table(&vec![entry]);
        
        // Tags should be separated by ", " (comma and space)
        assert!(table.contains("rust, cli"));
        assert!(!table.contains("rust::cli"));
    }

    // Test utility functions
    #[test]
    fn test_print_functions() {
        // These functions print to stdout/stderr, so we can't easily test output
        // But we can test that they don't panic
        print_success("Test success");
        print_error("Test error");
        print_warning("Test warning");
        print_info("Test info");
    }

    #[test]
    fn test_table_structure() {
        let entries = vec![create_test_entry(123, "Long Title for Testing Table Layout")];
        let table = format_entries_table(&entries);
        
        // Check that the table has proper structure
        assert!(table.contains("123")); // ID
        assert!(table.contains("Long Title for Testing Table Layout")); // Title
        
        // Verify that table formatting includes borders/separators
        // The exact format depends on the tabled crate configuration
        assert!(table.len() > 100); // Should be reasonably long with formatting
    }
}