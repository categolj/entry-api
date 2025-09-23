use thiserror::Error;

#[derive(Error, Debug)]
pub enum CliError {
    #[error("HTTP request failed: {0}")]
    Request(#[from] reqwest::Error),

    #[error("Serialization failed: {0}")]
    Serialization(#[from] serde_json::Error),

    #[error("IO error: {0}")]
    Io(#[from] std::io::Error),

    #[error("Configuration error: {0}")]
    Config(String),

    #[error("Authentication failed")]
    Authentication,

    #[error("Entry not found: {0}")]
    EntryNotFound(String),

    #[error("Invalid input: {0}")]
    InvalidInput(String),

    #[error("API error: {status} - {message}")]
    ApiError { status: u16, message: String },
}

impl CliError {
    pub fn config(msg: impl Into<String>) -> Self {
        Self::Config(msg.into())
    }

    pub fn invalid_input(msg: impl Into<String>) -> Self {
        Self::InvalidInput(msg.into())
    }

    pub fn api_error(status: u16, message: impl Into<String>) -> Self {
        Self::ApiError {
            status,
            message: message.into(),
        }
    }
}

pub type Result<T> = std::result::Result<T, CliError>;

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_error_creation() {
        let config_err = CliError::config("Invalid config");
        assert!(matches!(config_err, CliError::Config(_)));
        assert_eq!(config_err.to_string(), "Configuration error: Invalid config");

        let input_err = CliError::invalid_input("Bad input");
        assert!(matches!(input_err, CliError::InvalidInput(_)));
        assert_eq!(input_err.to_string(), "Invalid input: Bad input");

        let api_err = CliError::api_error(404, "Not found");
        assert!(matches!(api_err, CliError::ApiError { .. }));
        assert_eq!(api_err.to_string(), "API error: 404 - Not found");
    }

    #[test]
    fn test_error_display() {
        let auth_err = CliError::Authentication;
        assert_eq!(auth_err.to_string(), "Authentication failed");

        let entry_err = CliError::EntryNotFound("Entry 123".to_string());
        assert_eq!(entry_err.to_string(), "Entry not found: Entry 123");
    }
}