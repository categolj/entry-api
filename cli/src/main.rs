mod cli;
mod client;
mod commands;
mod config;
mod error;

use clap::Parser;
use cli::{Cli, Commands};
use client::EntryClient;
use commands::{category, config as config_cmd, entry, tag};
use config::Config;
use error::Result;

#[tokio::main]
async fn main() -> Result<()> {
    let cli = Cli::parse();

    // Handle config command first (doesn't need client)
    if let Commands::Config { command } = &cli.command {
        return config_cmd::handle_config_command(command.to_owned(), &cli.format).await;
    }

    // Load configuration
    let config = if let Some(config_path) = &cli.config {
        let contents = std::fs::read_to_string(config_path)?;
        toml::from_str(&contents).map_err(|e| {
            error::CliError::Config(format!("Failed to parse config file: {}", e))
        })?
    } else {
        Config::load().unwrap_or_default()
    };

    // Determine values with priority: CLI args > environment > profile > config file
    let url = cli
        .url
        .clone()
        .unwrap_or_else(|| config.get_host(cli.profile.as_deref()));

    let tenant = cli
        .tenant
        .clone()
        .unwrap_or_else(|| config.get_tenant(cli.profile.as_deref()));

    let (username, password) = match (&cli.user, &cli.password) {
        (Some(u), Some(p)) => (Some(u.clone()), Some(p.clone())),
        (Some(u), None) => {
            let (_, config_pass) = config.get_auth(cli.profile.as_deref());
            (Some(u.clone()), config_pass)
        }
        (None, Some(p)) => {
            let (config_user, _) = config.get_auth(cli.profile.as_deref());
            (config_user, Some(p.clone()))
        }
        (None, None) => config.get_auth(cli.profile.as_deref()),
    };

    // Create client
    let mut client = EntryClient::new(url.clone())?;
    
    if cli.verbose {
        println!("Using URL: {}", url);
        println!("Using tenant: {}", tenant);
        if let (Some(ref u), Some(_)) = (&username, &password) {
            println!("Using authentication: {}", u);
        } else {
            println!("No authentication configured");
        }
    }
    
    if tenant != "_" {
        client = client.with_tenant(tenant);
    }

    if let (Some(user), Some(pass)) = (username, password) {
        if !user.is_empty() && !pass.is_empty() {
            client = client.with_auth(user, pass);
        }
    }

    // Handle commands
    match cli.command {
        Commands::Entry { command } => {
            entry::handle_entry_command(command, client, &cli.format).await
        }
        Commands::Category { command } => {
            category::handle_category_command(command, client, &cli.format).await
        }
        Commands::Tag { command } => {
            tag::handle_tag_command(command, client, &cli.format).await
        }
        Commands::Config { .. } => unreachable!(),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn verify_cli() {
        use clap::CommandFactory;
        Cli::command().debug_assert();
    }
}