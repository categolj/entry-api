# Entry CLI

Command-line interface for Entry API written in Rust.

## Installation

```bash
cd cli
cargo build --release
# Binary will be available at target/release/entry-cli
```

## Configuration

Initialize configuration:
```bash
entry-cli config init
```

This creates a configuration file at `~/.config/entry-cli/config.toml`:

```toml
[default]
host = "http://localhost:8080"
tenant = "_"

[auth]
username = "admin"
# password = "changeme"  # Can be set via environment variable ENTRY_PASSWORD

[profiles.production]
host = "https://api.example.com"
tenant = "prod"
```

## Authentication

The CLI supports Basic Authentication. You can provide credentials in multiple ways:

1. **Configuration file**: Set in `~/.config/entry-cli/config.toml`
2. **Environment variables**: `ENTRY_USER` and `ENTRY_PASSWORD`
3. **Command line**: `--user` and `--password` options

Priority: CLI args > Environment variables > Config file

## Usage

### Entry Management

List entries:
```bash
entry-cli entry list
entry-cli entry list --tag "Spring Boot" --size 10
entry-cli entry list --query "REST API" --category Development
```

Get a single entry:
```bash
entry-cli entry get 123
entry-cli entry get 123 --markdown
entry-cli entry get 123 --format json
```

Create a new entry:
```bash
# From file
entry-cli entry create post.md

# From stdin
echo "# Title" | entry-cli entry create -

# Open editor
entry-cli entry create --edit
```

Update an entry:
```bash
# Update entire entry
entry-cli entry update 123 updated-post.md

# Update with editor
entry-cli entry update 123 --edit

# Update only summary
entry-cli entry update 123 --summary "New summary text"
```

Delete an entry:
```bash
entry-cli entry delete 123
entry-cli entry delete 123 --force  # Skip confirmation
```

Search entries:
```bash
entry-cli entry search "Spring Boot"
entry-cli entry search "REST API" --interactive
```

Get template:
```bash
entry-cli entry template > new-post.md
```

### Category Management

List all categories:
```bash
entry-cli category list
entry-cli category list --format json
```

### Tag Management

List all tags with usage count:
```bash
entry-cli tag list
entry-cli tag list --sort  # Sort by count
```

### Configuration Management

Show current configuration:
```bash
entry-cli config show
entry-cli config show --show-secrets
```

Set configuration values:
```bash
entry-cli config set default.host http://localhost:8080
entry-cli config set auth.username myuser
entry-cli config set profiles.staging.host https://staging.example.com
```

## Profiles

Use profiles to switch between different environments:

```bash
# Use production profile
entry-cli --profile production entry list

# Set default profile in environment
export ENTRY_PROFILE=production
```

## Output Formats

The CLI supports multiple output formats:
- `table` (default): Human-readable table format
- `json`: JSON output for scripting
- `markdown`: Markdown format (for entry content)

Example:
```bash
entry-cli entry list --format json | jq '.content[].entryId'
```

## Environment Variables

- `ENTRY_URL`: API endpoint URL
- `ENTRY_TENANT`: Tenant ID
- `ENTRY_USER`: Authentication username  
- `ENTRY_PASSWORD`: Authentication password
- `EDITOR`: Text editor for `--edit` option (default: vi)

## Examples

### Create and publish a blog post
```bash
# 1. Get template
entry-cli entry template > my-post.md

# 2. Edit the post
vim my-post.md

# 3. Create entry
entry-cli entry create my-post.md

# 4. Update if needed
entry-cli entry update 123 --edit
```

### Batch operations
```bash
# Export all entries
for id in $(entry-cli entry list --format json | jq -r '.content[].entryId'); do
  entry-cli entry get $id --markdown > "entry-$id.md"
done

# Find and update entries
entry-cli entry search "TODO" --format json | \
  jq -r '.content[].entryId' | \
  xargs -I {} entry-cli entry update {} --summary "Updated"
```

## Development

Run tests:
```bash
cargo test
```

Build debug version:
```bash
cargo build
```

## License

MIT