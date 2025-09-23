use crate::cli::{ConfigCommands, OutputFormat};
use crate::commands::{print_info, print_success, print_warning};
use crate::config::Config;
use crate::error::{CliError, Result};
use colored::Colorize;

pub async fn handle_config_command(command: ConfigCommands, _format: &OutputFormat) -> Result<()> {
    match command {
        ConfigCommands::Init { force } => init_config(force),
        ConfigCommands::Show { show_secrets } => show_config(show_secrets),
        ConfigCommands::Set { key, value } => set_config_value(&key, &value),
    }
}

fn init_config(force: bool) -> Result<()> {
    let config_path = Config::config_path()?;

    if config_path.exists() && !force {
        print_warning(&format!(
            "Configuration file already exists at: {}",
            config_path.display()
        ));
        print_info("Use --force to overwrite");
        return Ok(());
    }

    let config = Config::init_default()?;
    print_success(&format!(
        "Configuration initialized at: {}",
        config_path.display()
    ));

    println!("\nDefault configuration:");
    println!("  Host: {}", config.default.host);
    println!("  Tenant: {}", config.default.tenant);
    println!("\nExample profile 'production' has been added.");
    println!("\nYou can now set authentication credentials:");
    println!("  {} config set auth.username YOUR_USERNAME", "entry-cli".bold());
    println!("  {} config set auth.password YOUR_PASSWORD", "entry-cli".bold());

    Ok(())
}

fn show_config(show_secrets: bool) -> Result<()> {
    let config_path = Config::config_path()?;
    let config = Config::load()?;

    println!("{}", "Configuration".bold().underline());
    println!("Path: {}", config_path.display());
    println!();

    println!("{}", "Default:".bold());
    println!("  Host: {}", config.default.host);
    println!("  Tenant: {}", config.default.tenant);
    println!();

    println!("{}", "Authentication:".bold());
    println!(
        "  Username: {}",
        config
            .auth
            .username
            .as_ref()
            .map(|u| {
                if show_secrets {
                    u.clone()
                } else {
                    format!("{} (hidden)", &u[..u.len().min(3)])
                }
            })
            .unwrap_or_else(|| "(not set)".to_string())
    );
    println!(
        "  Password: {}",
        config
            .auth
            .password
            .as_ref()
            .map(|_| {
                if show_secrets {
                    "(set)".to_string()
                } else {
                    "(hidden)".to_string()
                }
            })
            .unwrap_or_else(|| "(not set)".to_string())
    );

    if !config.profiles.is_empty() {
        println!("\n{}", "Profiles:".bold());
        for (name, profile) in &config.profiles {
            println!("\n  [{}]", name.yellow());
            println!("    Host: {}", profile.host);
            println!("    Tenant: {}", profile.tenant);
            if profile.auth.username.is_some() || profile.auth.password.is_some() {
                println!("    Auth: (configured)");
            }
        }
    }

    if !show_secrets {
        println!();
        print_info("Use --show-secrets to display sensitive information");
    }

    Ok(())
}

fn set_config_value(key: &str, value: &str) -> Result<()> {
    let mut config = Config::load()?;

    let parts: Vec<&str> = key.split('.').collect();
    match parts.as_slice() {
        ["default", "host"] => config.default.host = value.to_string(),
        ["default", "tenant"] => config.default.tenant = value.to_string(),
        ["auth", "username"] => config.auth.username = Some(value.to_string()),
        ["auth", "password"] => {
            config.auth.password = Some(value.to_string());
            print_warning("Password stored in plain text. Consider using environment variables.");
        }
        ["profiles", profile_name, "host"] => {
            config
                .profiles
                .entry(profile_name.to_string())
                .or_insert_with(|| crate::config::ProfileConfig {
                    host: String::new(),
                    tenant: "_".to_string(),
                    auth: Default::default(),
                })
                .host = value.to_string();
        }
        ["profiles", profile_name, "tenant"] => {
            config
                .profiles
                .entry(profile_name.to_string())
                .or_insert_with(|| crate::config::ProfileConfig {
                    host: "http://localhost:8080".to_string(),
                    tenant: String::new(),
                    auth: Default::default(),
                })
                .tenant = value.to_string();
        }
        _ => {
            return Err(CliError::Config(format!("Invalid configuration key: {}", key)));
        }
    }

    config.save()?;
    print_success(&format!("Configuration updated: {} = {}", key, value));

    Ok(())
}