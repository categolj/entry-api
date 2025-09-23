use crate::cli::{OutputFormat, TagCommands};
use crate::client::EntryClient;
use crate::commands::{format_output, format_tags_table, print_info};
use crate::error::Result;
use colored::Colorize;

pub async fn handle_tag_command(
    command: TagCommands,
    client: EntryClient,
    format: &OutputFormat,
) -> Result<()> {
    match command {
        TagCommands::List { sort } => list_tags(client, sort, format).await,
    }
}

async fn list_tags(client: EntryClient, sort: bool, format: &OutputFormat) -> Result<()> {
    let mut tags = client.list_tags().await?;

    if sort {
        tags.sort_by(|a, b| b.count.cmp(&a.count));
    }

    match format {
        OutputFormat::Table => {
            if tags.is_empty() {
                print_info("No tags found");
            } else {
                println!("{}", "Tags".bold().underline());
                println!();
                println!("{}", format_tags_table(&tags));
                println!();
                
                let total_count: u32 = tags.iter().map(|t| t.count).sum();
                println!(
                    "Total: {} unique tags, {} total usage",
                    tags.len(),
                    total_count
                );
            }
        }
        _ => println!("{}", format_output(&tags, format)),
    }

    Ok(())
}