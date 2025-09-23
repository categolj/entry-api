use crate::cli::{EntryCommands, OutputFormat};
use crate::client::{models::*, EntryClient};
use crate::commands::{format_entries_table, format_output, print_error, print_info, print_success};
use crate::error::Result;
use colored::Colorize;
use std::io::{self, Read, Write};
use std::process::{Command, Stdio};

pub async fn handle_entry_command(
    command: EntryCommands,
    client: EntryClient,
    format: &OutputFormat,
) -> Result<()> {
    match command {
        EntryCommands::List {
            query,
            category,
            tag,
            size,
            cursor,
        } => list_entries(client, query, category, tag, size, cursor, format).await,

        EntryCommands::Get { id, markdown } => get_entry(client, id, markdown, format).await,

        EntryCommands::Create { file, edit } => create_entry(client, file, edit).await,

        EntryCommands::Update {
            id,
            file,
            edit,
            summary,
        } => update_entry(client, id, file, edit, summary).await,

        EntryCommands::Delete { id, force } => delete_entry(client, id, force).await,

        EntryCommands::Search { query, interactive } => {
            search_entries(client, query, interactive, format).await
        }

        EntryCommands::Template => get_template(client).await,
    }
}

async fn list_entries(
    client: EntryClient,
    query: Option<String>,
    categories: Vec<String>,
    tag: Option<String>,
    size: u32,
    cursor: Option<String>,
    format: &OutputFormat,
) -> Result<()> {
    let criteria = SearchCriteria {
        query,
        categories,
        tag,
    };

    let page_request = PageRequest {
        cursor,
        size: Some(size),
        direction: None,
    };

    let page = client.list_entries(&criteria, &page_request).await?;

    match format {
        OutputFormat::Table => {
            if page.content.is_empty() {
                print_info("No entries found");
            } else {
                println!("{}", format_entries_table(&page.content));
                println!(
                    "\nShowing {} entries (Page size: {})",
                    page.content.len(),
                    page.size
                );

                if page.has_next {
                    print_info("More entries available. Use --cursor to paginate.");
                }
            }
        }
        _ => println!("{}", format_output(&page, format)),
    }

    Ok(())
}

async fn get_entry(
    client: EntryClient,
    id: u64,
    markdown: bool,
    format: &OutputFormat,
) -> Result<()> {
    if markdown {
        let content = client.get_entry_markdown(id).await?;
        println!("{}", content);
    } else {
        let entry = client.get_entry(id).await?;

        match format {
            OutputFormat::Markdown => println!("{}", entry.to_markdown()),
            OutputFormat::Table => {
                println!("{}", "Entry Details".bold().underline());
                println!("ID:         {}", entry.entry_id);
                println!("Tenant:     {}", entry.tenant_id);
                println!("Title:      {}", entry.front_matter.title);
                println!("Summary:    {}", entry.front_matter.summary);
                println!(
                    "Categories: {}",
                    entry
                        .front_matter
                        .categories
                        .iter()
                        .map(|c| c.name.as_str())
                        .collect::<Vec<_>>()
                        .join("::")
                );
                println!(
                    "Tags:       {}",
                    entry
                        .front_matter
                        .tags
                        .iter()
                        .map(|t| t.name.as_str())
                        .collect::<Vec<_>>()
                        .join(", ")
                );
                println!("Created:    {} by {}", 
                    entry.created.date
                        .map(|d| d.format("%Y-%m-%d %H:%M:%S").to_string())
                        .unwrap_or_else(|| "N/A".to_string()),
                    entry.created.name
                );
                println!("Updated:    {} by {}", 
                    entry.updated.date
                        .map(|d| d.format("%Y-%m-%d %H:%M:%S").to_string())
                        .unwrap_or_else(|| "N/A".to_string()),
                    entry.updated.name
                );

                if let Some(content) = &entry.content {
                    println!("\n{}", "Content:".bold());
                    println!("{}", content);
                }
            }
            _ => println!("{}", format_output(&entry, format)),
        }
    }

    Ok(())
}

async fn create_entry(client: EntryClient, file: String, edit: bool) -> Result<()> {
    let markdown = if edit {
        // Get template first
        let template = client.get_template().await.unwrap_or_else(|_| {
            "---\ntitle: New Entry\nsummary: \ntags: []\ncategories: []\n---\n\n".to_string()
        });

        open_editor(&template)?
    } else if file == "-" {
        let mut buffer = String::new();
        io::stdin().read_to_string(&mut buffer)?;
        buffer
    } else {
        std::fs::read_to_string(&file)?
    };

    let entry = client.create_entry(&markdown).await?;
    print_success(&format!("Entry created with ID: {}", entry.entry_id));

    Ok(())
}

async fn update_entry(
    client: EntryClient,
    id: u64,
    file: String,
    edit: bool,
    summary: Option<String>,
) -> Result<()> {
    if let Some(summary_text) = summary {
        // Update only summary
        let entry = client.update_entry_summary(id, &summary_text).await?;
        print_success(&format!("Entry {} summary updated", entry.entry_id));
        return Ok(());
    }

    let markdown = if edit {
        // Get current content
        let current = client.get_entry_markdown(id).await?;
        open_editor(&current)?
    } else if file == "-" {
        let mut buffer = String::new();
        io::stdin().read_to_string(&mut buffer)?;
        buffer
    } else {
        std::fs::read_to_string(&file)?
    };

    let entry = client.update_entry(id, &markdown).await?;
    print_success(&format!("Entry {} updated", entry.entry_id));

    Ok(())
}

async fn delete_entry(client: EntryClient, id: u64, force: bool) -> Result<()> {
    if !force {
        print!("Are you sure you want to delete entry {}? (y/N): ", id);
        io::stdout().flush()?;

        let mut response = String::new();
        io::stdin().read_line(&mut response)?;

        if !response.trim().to_lowercase().starts_with('y') {
            print_info("Deletion cancelled");
            return Ok(());
        }
    }

    client.delete_entry(id).await?;
    print_success(&format!("Entry {} deleted", id));

    Ok(())
}

async fn search_entries(
    client: EntryClient,
    query: String,
    interactive: bool,
    format: &OutputFormat,
) -> Result<()> {
    let criteria = SearchCriteria {
        query: Some(query.clone()),
        categories: vec![],
        tag: None,
    };

    let page_request = PageRequest {
        cursor: None,
        size: Some(50),
        direction: None,
    };

    let page = client.list_entries(&criteria, &page_request).await?;

    if page.content.is_empty() {
        print_info(&format!("No entries found for query: {}", query));
        return Ok(());
    }

    match format {
        OutputFormat::Table => {
            println!("{}", format_entries_table(&page.content));
            println!(
                "\nFound {} entries matching '{}'",
                page.content.len(),
                query
            );

            if interactive && !page.content.is_empty() {
                print!("\nEnter entry ID to view (or press Enter to skip): ");
                io::stdout().flush()?;

                let mut input = String::new();
                io::stdin().read_line(&mut input)?;

                if let Ok(id) = input.trim().parse::<u64>() {
                    if page.content.iter().any(|e| e.entry_id == id) {
                        println!();
                        get_entry(client, id, false, format).await?;
                    } else {
                        print_error("Invalid entry ID");
                    }
                }
            }
        }
        _ => println!("{}", format_output(&page, format)),
    }

    Ok(())
}

async fn get_template(client: EntryClient) -> Result<()> {
    let template = client.get_template().await?;
    println!("{}", template);
    Ok(())
}

fn open_editor(initial_content: &str) -> Result<String> {
    let editor = std::env::var("EDITOR").unwrap_or_else(|_| "vi".to_string());

    let temp_file = tempfile::NamedTempFile::new()?;
    let temp_path = temp_file.path().to_path_buf();
    
    // Write initial content
    std::fs::write(&temp_path, initial_content)?;

    let status = Command::new(&editor)
        .arg(&temp_path)
        .stdin(Stdio::inherit())
        .stdout(Stdio::inherit())
        .stderr(Stdio::inherit())
        .status()?;

    if !status.success() {
        return Err(crate::error::CliError::InvalidInput(
            "Editor exited with error".to_string(),
        ));
    }

    // Read the edited content
    let content = std::fs::read_to_string(&temp_path)?;
    Ok(content)
}