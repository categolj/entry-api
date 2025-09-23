use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Entry {
    pub entry_id: u64,
    pub tenant_id: String,
    pub front_matter: FrontMatter,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub content: Option<String>,
    pub created: Author,
    pub updated: Author,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FrontMatter {
    pub title: String,
    pub summary: String,
    pub categories: Vec<Category>,
    pub tags: Vec<Tag>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Category {
    pub name: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Tag {
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TagAndCount {
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub version: Option<String>,
    pub count: u32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Author {
    pub name: String,
    pub date: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CursorPage<T> {
    pub content: Vec<T>,
    pub size: u32,
    pub has_previous: bool,
    pub has_next: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub next_cursor: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub previous_cursor: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProblemDetail {
    pub detail: String,
    pub instance: String,
    pub status: u16,
    pub title: String,
    #[serde(rename = "type")]
    pub problem_type: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
pub struct SearchCriteria {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub query: Option<String>,
    #[serde(skip_serializing_if = "Vec::is_empty")]
    pub categories: Vec<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub tag: Option<String>,
}

#[derive(Debug, Clone, Serialize)]
pub struct PageRequest {
    #[serde(skip_serializing_if = "Option::is_none")]
    pub cursor: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub size: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub direction: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct EntrySummaryPatchRequest {
    pub summary: String,
}

impl Entry {
    pub fn to_markdown(&self) -> String {
        let categories_str = self
            .front_matter
            .categories
            .iter()
            .map(|c| format!("\"{}\"", c.name))
            .collect::<Vec<_>>()
            .join(", ");

        let tags_str = self
            .front_matter
            .tags
            .iter()
            .map(|t| format!("\"{}\"", t.name))
            .collect::<Vec<_>>()
            .join(", ");

        let mut front_matter = format!(
            "---\ntitle: {}\nsummary: {}\ntags: [{}]\ncategories: [{}]",
            self.front_matter.title,
            self.front_matter.summary,
            tags_str,
            categories_str
        );

        if let Some(date) = &self.created.date {
            front_matter.push_str(&format!("\ndate: {}", date.to_rfc3339()));
        }

        if let Some(date) = &self.updated.date {
            front_matter.push_str(&format!("\nupdated: {}", date.to_rfc3339()));
        }

        front_matter.push_str("\n---\n\n");

        if let Some(content) = &self.content {
            front_matter.push_str(content);
        }

        front_matter
    }
}