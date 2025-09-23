use crate::error::{CliError, Result};
use dirs::config_dir;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fs;
use std::path::PathBuf;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Config {
    #[serde(default)]
    pub default: DefaultConfig,
    #[serde(default)]
    pub auth: AuthConfig,
    #[serde(default)]
    pub profiles: HashMap<String, ProfileConfig>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DefaultConfig {
    #[serde(default = "default_host")]
    pub host: String,
    #[serde(default = "default_tenant")]
    pub tenant: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AuthConfig {
    pub username: Option<String>,
    pub password: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ProfileConfig {
    pub host: String,
    pub tenant: String,
    #[serde(default)]
    pub auth: AuthConfig,
}

fn default_host() -> String {
    "http://localhost:8080".to_string()
}

fn default_tenant() -> String {
    "_".to_string()
}

impl Default for Config {
    fn default() -> Self {
        Self {
            default: DefaultConfig::default(),
            auth: AuthConfig::default(),
            profiles: HashMap::new(),
        }
    }
}

impl Default for DefaultConfig {
    fn default() -> Self {
        Self {
            host: default_host(),
            tenant: default_tenant(),
        }
    }
}

impl Default for AuthConfig {
    fn default() -> Self {
        Self {
            username: None,
            password: None,
        }
    }
}

impl Config {
    pub fn config_path() -> Result<PathBuf> {
        let config_dir = config_dir()
            .ok_or_else(|| CliError::Config("Could not find config directory".to_string()))?;
        Ok(config_dir.join("entry-cli").join("config.toml"))
    }

    pub fn load() -> Result<Self> {
        let config_path = Self::config_path()?;
        
        if !config_path.exists() {
            return Ok(Config::default());
        }

        let contents = fs::read_to_string(&config_path)?;
        let config: Config = toml::from_str(&contents)
            .map_err(|e| CliError::Config(format!("Invalid config file: {}", e)))?;

        Ok(config)
    }

    pub fn save(&self) -> Result<()> {
        let config_path = Self::config_path()?;
        
        if let Some(parent) = config_path.parent() {
            fs::create_dir_all(parent)?;
        }

        let toml_str = toml::to_string_pretty(self)
            .map_err(|e| CliError::Config(format!("Failed to serialize config: {}", e)))?;
        
        fs::write(&config_path, toml_str)?;
        
        Ok(())
    }

    pub fn init_default() -> Result<Self> {
        let mut profiles = HashMap::new();
        
        // Add example production profile
        profiles.insert(
            "production".to_string(),
            ProfileConfig {
                host: "https://api.example.com".to_string(),
                tenant: "prod".to_string(),
                auth: AuthConfig::default(),
            },
        );

        let config = Self {
            default: DefaultConfig::default(),
            auth: AuthConfig::default(),
            profiles,
        };

        config.save()?;
        Ok(config)
    }

    pub fn get_profile(&self, name: &str) -> Option<&ProfileConfig> {
        self.profiles.get(name)
    }

    pub fn get_host(&self, profile: Option<&str>) -> String {
        profile
            .and_then(|p| self.get_profile(p))
            .map(|p| p.host.clone())
            .unwrap_or_else(|| self.default.host.clone())
    }

    pub fn get_tenant(&self, profile: Option<&str>) -> String {
        profile
            .and_then(|p| self.get_profile(p))
            .map(|p| p.tenant.clone())
            .unwrap_or_else(|| self.default.tenant.clone())
    }

    pub fn get_auth(&self, profile: Option<&str>) -> (Option<String>, Option<String>) {
        let auth = profile
            .and_then(|p| self.get_profile(p))
            .map(|p| &p.auth)
            .unwrap_or(&self.auth);

        (auth.username.clone(), auth.password.clone())
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;
    use tempfile::TempDir;

    fn create_test_config(temp_dir: &TempDir) -> PathBuf {
        let config_dir = temp_dir.path().join("entry-cli");
        fs::create_dir_all(&config_dir).unwrap();
        config_dir.join("config.toml")
    }

    #[test]
    fn test_default_config() {
        let config = Config::default();
        assert_eq!(config.default.host, "http://localhost:8080");
        assert_eq!(config.default.tenant, "_");
        assert!(config.auth.username.is_none());
        assert!(config.auth.password.is_none());
        assert!(config.profiles.is_empty());
    }

    #[test]
    fn test_config_serialization() {
        let mut config = Config::default();
        config.auth.username = Some("testuser".to_string());
        config.auth.password = Some("testpass".to_string());
        
        config.profiles.insert(
            "prod".to_string(),
            ProfileConfig {
                host: "https://api.example.com".to_string(),
                tenant: "production".to_string(),
                auth: AuthConfig {
                    username: Some("produser".to_string()),
                    password: Some("prodpass".to_string()),
                },
            },
        );

        let toml_str = toml::to_string_pretty(&config).unwrap();
        let deserialized: Config = toml::from_str(&toml_str).unwrap();

        assert_eq!(config.default.host, deserialized.default.host);
        assert_eq!(config.auth.username, deserialized.auth.username);
        assert_eq!(config.profiles.len(), deserialized.profiles.len());
        
        let prod_profile = deserialized.get_profile("prod").unwrap();
        assert_eq!(prod_profile.host, "https://api.example.com");
        assert_eq!(prod_profile.tenant, "production");
    }

    #[test]
    fn test_get_host() {
        let mut config = Config::default();
        config.profiles.insert(
            "test".to_string(),
            ProfileConfig {
                host: "https://test.example.com".to_string(),
                tenant: "test".to_string(),
                auth: AuthConfig::default(),
            },
        );

        // Default host
        assert_eq!(config.get_host(None), "http://localhost:8080");
        
        // Profile host
        assert_eq!(config.get_host(Some("test")), "https://test.example.com");
        
        // Non-existent profile falls back to default
        assert_eq!(config.get_host(Some("nonexistent")), "http://localhost:8080");
    }

    #[test]
    fn test_get_tenant() {
        let mut config = Config::default();
        config.profiles.insert(
            "custom".to_string(),
            ProfileConfig {
                host: "https://api.example.com".to_string(),
                tenant: "custom-tenant".to_string(),
                auth: AuthConfig::default(),
            },
        );

        // Default tenant
        assert_eq!(config.get_tenant(None), "_");
        
        // Profile tenant
        assert_eq!(config.get_tenant(Some("custom")), "custom-tenant");
        
        // Non-existent profile falls back to default
        assert_eq!(config.get_tenant(Some("nonexistent")), "_");
    }

    #[test]
    fn test_get_auth() {
        let mut config = Config::default();
        config.auth.username = Some("default_user".to_string());
        config.auth.password = Some("default_pass".to_string());
        
        config.profiles.insert(
            "secure".to_string(),
            ProfileConfig {
                host: "https://secure.example.com".to_string(),
                tenant: "secure".to_string(),
                auth: AuthConfig {
                    username: Some("secure_user".to_string()),
                    password: Some("secure_pass".to_string()),
                },
            },
        );

        // Default auth
        let (username, password) = config.get_auth(None);
        assert_eq!(username, Some("default_user".to_string()));
        assert_eq!(password, Some("default_pass".to_string()));
        
        // Profile auth
        let (username, password) = config.get_auth(Some("secure"));
        assert_eq!(username, Some("secure_user".to_string()));
        assert_eq!(password, Some("secure_pass".to_string()));
        
        // Non-existent profile falls back to default
        let (username, password) = config.get_auth(Some("nonexistent"));
        assert_eq!(username, Some("default_user".to_string()));
        assert_eq!(password, Some("default_pass".to_string()));
    }

    #[test]
    fn test_config_save_and_load() {
        let temp_dir = TempDir::new().unwrap();
        let config_path = create_test_config(&temp_dir);

        let mut original_config = Config::default();
        original_config.default.host = "https://test.example.com".to_string();
        original_config.auth.username = Some("testuser".to_string());

        // Save config
        let toml_content = toml::to_string_pretty(&original_config).unwrap();
        fs::write(&config_path, toml_content).unwrap();

        // Load and verify
        let loaded_content = fs::read_to_string(&config_path).unwrap();
        let loaded_config: Config = toml::from_str(&loaded_content).unwrap();

        assert_eq!(original_config.default.host, loaded_config.default.host);
        assert_eq!(original_config.auth.username, loaded_config.auth.username);
    }

    #[test]
    fn test_init_default() {
        let config = Config {
            default: DefaultConfig::default(),
            auth: AuthConfig::default(),
            profiles: {
                let mut profiles = HashMap::new();
                profiles.insert(
                    "production".to_string(),
                    ProfileConfig {
                        host: "https://api.example.com".to_string(),
                        tenant: "prod".to_string(),
                        auth: AuthConfig::default(),
                    },
                );
                profiles
            },
        };

        assert!(config.profiles.contains_key("production"));
        let prod_profile = config.get_profile("production").unwrap();
        assert_eq!(prod_profile.host, "https://api.example.com");
        assert_eq!(prod_profile.tenant, "prod");
    }

    #[test]
    fn test_invalid_toml_handling() {
        let invalid_toml = "invalid toml content [[[";
        let result: std::result::Result<Config, _> = toml::from_str(invalid_toml);
        assert!(result.is_err());
    }

    #[test]
    fn test_partial_config() {
        // Test loading config with missing fields (should use defaults)
        let partial_toml = r#"
[auth]
username = "user"

[profiles.test]
host = "https://test.example.com"
tenant = "test"
"#;
        
        let config: Config = toml::from_str(partial_toml).unwrap();
        assert_eq!(config.default.host, "http://localhost:8080"); // default
        assert_eq!(config.default.tenant, "_"); // default
        assert_eq!(config.auth.username, Some("user".to_string()));
        assert!(config.auth.password.is_none());
        
        let test_profile = config.get_profile("test").unwrap();
        assert_eq!(test_profile.host, "https://test.example.com");
        assert_eq!(test_profile.tenant, "test");
        assert!(test_profile.auth.username.is_none());
    }
}