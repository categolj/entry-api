use clap::{Parser, Subcommand};

#[derive(Parser)]
#[command(name = "entry-cli")]
#[command(about = "Command line interface for Entry API", long_about = None)]
#[command(version)]
pub struct Cli {
    /// Configuration file path
    #[arg(long, global = true)]
    pub config: Option<String>,

    /// Tenant ID
    #[arg(short, long, global = true, env = "ENTRY_TENANT")]
    pub tenant: Option<String>,

    /// API endpoint URL
    #[arg(long, global = true, env = "ENTRY_URL")]
    pub url: Option<String>,

    /// Authentication username
    #[arg(short, long, global = true, env = "ENTRY_USER")]
    pub user: Option<String>,

    /// Authentication password
    #[arg(short, long, global = true, env = "ENTRY_PASSWORD")]
    pub password: Option<String>,

    /// Use configuration profile
    #[arg(long, global = true)]
    pub profile: Option<String>,

    /// Output format (json, table, markdown)
    #[arg(long, global = true, default_value = "table")]
    pub format: OutputFormat,

    /// Verbose output for debugging
    #[arg(short, long, global = true)]
    pub verbose: bool,

    #[command(subcommand)]
    pub command: Commands,
}

#[derive(Clone, Debug, PartialEq, clap::ValueEnum)]
pub enum OutputFormat {
    Json,
    Table,
    Markdown,
}

#[derive(Subcommand)]
pub enum Commands {
    /// Entry management
    Entry {
        #[command(subcommand)]
        command: EntryCommands,
    },

    /// Category management
    Category {
        #[command(subcommand)]
        command: CategoryCommands,
    },

    /// Tag management
    Tag {
        #[command(subcommand)]
        command: TagCommands,
    },

    /// Configuration management
    Config {
        #[command(subcommand)]
        command: ConfigCommands,
    },
}

#[derive(Subcommand)]
pub enum EntryCommands {
    /// List entries
    List {
        /// Search query
        #[arg(short, long)]
        query: Option<String>,

        /// Filter by category
        #[arg(short, long)]
        category: Vec<String>,

        /// Filter by tag
        #[arg(long)]
        tag: Option<String>,

        /// Page size
        #[arg(short, long, default_value = "20")]
        size: u32,

        /// Cursor for pagination
        #[arg(long)]
        cursor: Option<String>,
    },

    /// Get a single entry
    Get {
        /// Entry ID
        id: u64,

        /// Get as markdown
        #[arg(short, long)]
        markdown: bool,
    },

    /// Create a new entry
    Create {
        /// Markdown file path (use - for stdin)
        #[arg(default_value = "-")]
        file: String,

        /// Open editor to create entry
        #[arg(short, long)]
        edit: bool,
    },

    /// Update an existing entry
    Update {
        /// Entry ID
        id: u64,

        /// Markdown file path (use - for stdin)
        #[arg(default_value = "-")]
        file: String,

        /// Open editor to update entry
        #[arg(short, long)]
        edit: bool,

        /// Update only summary
        #[arg(short, long)]
        summary: Option<String>,
    },

    /// Delete an entry
    Delete {
        /// Entry ID
        id: u64,

        /// Skip confirmation
        #[arg(short, long)]
        force: bool,
    },

    /// Search entries
    Search {
        /// Search query
        query: String,

        /// Interactive mode
        #[arg(short, long)]
        interactive: bool,
    },

    /// Get template for new entry
    Template,
}

#[derive(Subcommand)]
pub enum CategoryCommands {
    /// List all categories
    List,
}

#[derive(Subcommand)]
pub enum TagCommands {
    /// List all tags with count
    List {
        /// Sort by count
        #[arg(short, long)]
        sort: bool,
    },
}

#[derive(Clone, Subcommand)]
pub enum ConfigCommands {
    /// Initialize configuration
    Init {
        /// Force overwrite existing config
        #[arg(short, long)]
        force: bool,
    },

    /// Show current configuration
    Show {
        /// Show full config including secrets
        #[arg(long)]
        show_secrets: bool,
    },

    /// Set configuration value
    Set {
        /// Configuration key (e.g., default.host, auth.username)
        key: String,
        
        /// Configuration value
        value: String,
    },
}