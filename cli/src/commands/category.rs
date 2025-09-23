use crate::cli::{CategoryCommands, OutputFormat};
use crate::client::EntryClient;
use crate::commands::{format_output, print_info};
use crate::error::Result;
use colored::Colorize;

pub async fn handle_category_command(
    command: CategoryCommands,
    client: EntryClient,
    format: &OutputFormat,
) -> Result<()> {
    match command {
        CategoryCommands::List => list_categories(client, format).await,
    }
}

async fn list_categories(client: EntryClient, format: &OutputFormat) -> Result<()> {
    let categories = client.list_categories().await?;

    match format {
        OutputFormat::Table => {
            if categories.is_empty() {
                print_info("No categories found");
            } else {
                println!("{}", "Categories".bold().underline());
                println!();

                for category_path in &categories {
                    let path = category_path
                        .iter()
                        .map(|c| c.name.as_str())
                        .collect::<Vec<_>>()
                        .join(" > ");
                    println!("  {}", path);
                }

                println!();
                println!("Total: {} category paths", categories.len());
            }
        }
        _ => println!("{}", format_output(&categories, format)),
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use crate::client::models::Category;

    #[test]
    fn test_category_path_separator() {
        let category_path = vec![
            Category { name: "tech".to_string() },
            Category { name: "programming".to_string() },
            Category { name: "rust".to_string() },
        ];

        let path = category_path
            .iter()
            .map(|c| c.name.as_str())
            .collect::<Vec<_>>()
            .join(" > ");

        assert_eq!(path, "tech > programming > rust");
        assert!(!path.contains("::"));
    }

    #[test]
    fn test_single_category() {
        let category_path = vec![
            Category { name: "blog".to_string() },
        ];

        let path = category_path
            .iter()
            .map(|c| c.name.as_str())
            .collect::<Vec<_>>()
            .join(" > ");

        assert_eq!(path, "blog");
    }

    #[test]
    fn test_empty_category_path() {
        let category_path: Vec<Category> = vec![];

        let path = category_path
            .iter()
            .map(|c| c.name.as_str())
            .collect::<Vec<_>>()
            .join(" > ");

        assert_eq!(path, "");
    }
}