pub mod models;

use crate::error::{CliError, Result};
use models::*;
use reqwest::{Client, RequestBuilder, Response, StatusCode};
use serde::de::DeserializeOwned;

pub struct EntryClient {
    client: Client,
    base_url: String,
    tenant_id: Option<String>,
    auth: Option<(String, String)>,
}

impl EntryClient {
    pub fn new(base_url: String) -> Result<Self> {
        let user_agent = format!("{}/{}", env!("CARGO_PKG_NAME"), env!("CARGO_PKG_VERSION"));
        let client = Client::builder()
            .user_agent(user_agent)
            .timeout(std::time::Duration::from_secs(30))
            .build()?;

        Ok(Self {
            client,
            base_url,
            tenant_id: None,
            auth: None,
        })
    }

    pub fn with_tenant(mut self, tenant_id: String) -> Self {
        self.tenant_id = Some(tenant_id);
        self
    }

    pub fn with_auth(mut self, username: String, password: String) -> Self {
        self.auth = Some((username, password));
        self
    }

    fn build_url(&self, path: &str) -> String {
        let url = match &self.tenant_id {
            Some(tenant_id) if tenant_id != "_" => {
                format!("{}/tenants/{}{}", self.base_url, tenant_id, path)
            }
            _ => format!("{}{}", self.base_url, path),
        };
        url
    }

    fn prepare_request(&self, request: RequestBuilder) -> RequestBuilder {
        match &self.auth {
            Some((username, password)) if !username.is_empty() && !password.is_empty() => {
                request.basic_auth(username, Some(password))
            },
            _ => request,
        }
    }

    async fn handle_response<T: DeserializeOwned>(response: Response) -> Result<T> {
        let status = response.status();
        
        if status.is_success() {
            Ok(response.json().await?)
        } else if status == StatusCode::NOT_FOUND {
            let problem = response.json::<ProblemDetail>().await?;
            Err(CliError::EntryNotFound(problem.detail))
        } else {
            // Try to parse as ProblemDetail first, fallback to status text
            match response.json::<ProblemDetail>().await {
                Ok(problem) => Err(CliError::ApiError {
                    status: status.as_u16(),
                    message: problem.detail,
                }),
                Err(_) => Err(CliError::ApiError {
                    status: status.as_u16(),
                    message: format!("HTTP {}: {}", status.as_u16(), status.canonical_reason().unwrap_or("Unknown error")),
                }),
            }
        }
    }

    pub async fn list_entries(
        &self,
        criteria: &SearchCriteria,
        page: &PageRequest,
    ) -> Result<CursorPage<Entry>> {
        let mut request = self
            .client
            .get(self.build_url("/entries"))
            .query(criteria)
            .query(page);

        request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn get_entry(&self, entry_id: u64) -> Result<Entry> {
        let request = self
            .client
            .get(self.build_url(&format!("/entries/{}", entry_id)));

        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn get_entry_markdown(&self, entry_id: u64) -> Result<String> {
        let request = self
            .client
            .get(self.build_url(&format!("/entries/{}.md", entry_id)));

        let request = self.prepare_request(request);
        let response = request.send().await?;
        let status = response.status();
        
        if status.is_success() {
            Ok(response.text().await?)
        } else {
            let problem = response.json::<ProblemDetail>().await?;
            Err(CliError::ApiError {
                status: status.as_u16(),
                message: problem.detail,
            })
        }
    }

    pub async fn create_entry(&self, markdown: &str) -> Result<Entry> {
        let request = self
            .client
            .post(self.build_url("/entries"))
            .header("Content-Type", "text/markdown")
            .body(markdown.to_string());

        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn update_entry(&self, entry_id: u64, markdown: &str) -> Result<Entry> {
        let request = self
            .client
            .put(self.build_url(&format!("/entries/{}", entry_id)))
            .header("Content-Type", "text/markdown")
            .body(markdown.to_string());

        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn update_entry_summary(
        &self,
        entry_id: u64,
        summary: &str,
    ) -> Result<Entry> {
        let request_body = EntrySummaryPatchRequest {
            summary: summary.to_string(),
        };

        let request = self
            .client
            .patch(self.build_url(&format!("/entries/{}/summary", entry_id)))
            .json(&request_body);

        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn delete_entry(&self, entry_id: u64) -> Result<()> {
        let request = self
            .client
            .delete(self.build_url(&format!("/entries/{}", entry_id)));

        let request = self.prepare_request(request);
        let response = request.send().await?;
        let status = response.status();

        if status == StatusCode::NO_CONTENT {
            Ok(())
        } else {
            let problem = response.json::<ProblemDetail>().await?;
            Err(CliError::ApiError {
                status: status.as_u16(),
                message: problem.detail,
            })
        }
    }

    pub async fn list_categories(&self) -> Result<Vec<Vec<Category>>> {
        let request = self.client.get(self.build_url("/categories"));
        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn list_tags(&self) -> Result<Vec<TagAndCount>> {
        let request = self.client.get(self.build_url("/tags"));
        let request = self.prepare_request(request);
        let response = request.send().await?;
        Self::handle_response(response).await
    }

    pub async fn get_template(&self) -> Result<String> {
        let response = self
            .client
            .get(format!("{}/entries/template.md", self.base_url))
            .send()
            .await?;
        let status = response.status();

        if status.is_success() {
            Ok(response.text().await?)
        } else {
            let problem = response.json::<ProblemDetail>().await?;
            Err(CliError::ApiError {
                status: status.as_u16(),
                message: problem.detail,
            })
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use mockito::Server;

    #[tokio::test]
    async fn test_client_creation() {
        let client = EntryClient::new("http://localhost:8080".to_string());
        assert!(client.is_ok());
    }

    #[tokio::test]
    async fn test_url_building() {
        let client = EntryClient::new("http://localhost:8080".to_string()).unwrap();
        
        // Default tenant
        assert_eq!(client.build_url("/entries"), "http://localhost:8080/entries");
        
        // Explicit default tenant
        let client = client.with_tenant("_".to_string());
        assert_eq!(client.build_url("/entries"), "http://localhost:8080/entries");
        
        // Custom tenant
        let client = EntryClient::new("http://localhost:8080".to_string())
            .unwrap()
            .with_tenant("custom".to_string());
        assert_eq!(client.build_url("/entries"), "http://localhost:8080/tenants/custom/entries");
    }

    #[tokio::test]
    async fn test_list_entries_success() {
        let mut server = Server::new_async().await;
        let _mock = server
            .mock("GET", "/entries")
            .match_query(mockito::Matcher::Any)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(r#"{
                "content": [],
                "size": 20,
                "hasPrevious": false,
                "hasNext": false,
                "cursor": null
            }"#)
            .create_async()
            .await;

        let client = EntryClient::new(server.url()).unwrap();
        let criteria = SearchCriteria {
            query: None,
            categories: vec![],
            tag: None,
        };
        let page_request = PageRequest {
            cursor: None,
            size: Some(20),
            direction: None,
        };

        let result = client.list_entries(&criteria, &page_request).await;
        if let Err(ref e) = result {
            eprintln!("Test failed with error: {:?}", e);
        }
        assert!(result.is_ok());
        
        let page = result.unwrap();
        assert_eq!(page.content.len(), 0);
        assert_eq!(page.size, 20);
        assert!(!page.has_next);
    }

    #[tokio::test]
    async fn test_get_entry_not_found() {
        let mut server = Server::new_async().await;
        let _mock = server
            .mock("GET", "/entries/999")
            .with_status(404)
            .with_header("content-type", "application/json")
            .with_body(r#"{
                "type": "about:blank",
                "title": "Not Found",
                "status": 404,
                "detail": "Entry not found: 999",
                "instance": "/entries/999"
            }"#)
            .create_async()
            .await;

        let client = EntryClient::new(server.url()).unwrap();
        let result = client.get_entry(999).await;
        
        assert!(result.is_err());
        match result.unwrap_err() {
            CliError::EntryNotFound(msg) => {
                assert_eq!(msg, "Entry not found: 999");
            }
            other => {
                eprintln!("Got unexpected error: {:?}", other);
                panic!("Expected EntryNotFound error");
            }
        }

        // mock.assert_async().await;
    }

    #[tokio::test]
    async fn test_api_error_handling() {
        let mut server = Server::new_async().await;
        let _mock = server
            .mock("GET", "/entries/123")
            .with_status(500)
            .with_header("content-type", "application/json")
            .with_body(r#"{
                "type": "about:blank",
                "title": "Internal Server Error",
                "status": 500,
                "detail": "Database connection failed",
                "instance": "/entries/123"
            }"#)
            .create_async()
            .await;

        let client = EntryClient::new(server.url()).unwrap();
        let result = client.get_entry(123).await;
        
        assert!(result.is_err());
        match result.unwrap_err() {
            CliError::ApiError { status, message } => {
                assert_eq!(status, 500);
                assert_eq!(message, "Database connection failed");
            }
            other => {
                eprintln!("Got unexpected error: {:?}", other);
                panic!("Expected ApiError");
            }
        }

        // mock.assert_async().await;
    }

    #[tokio::test]
    async fn test_authentication() {
        let mut server = Server::new_async().await;
        let _mock = server
            .mock("GET", "/entries")
            .match_header("authorization", "Basic dGVzdDpwYXNz") // test:pass in base64
            .match_query(mockito::Matcher::Any)
            .with_status(200)
            .with_header("content-type", "application/json")
            .with_body(r#"{
                "content": [],
                "size": 20,
                "hasPrevious": false,
                "hasNext": false,
                "cursor": null
            }"#)
            .create_async()
            .await;

        let client = EntryClient::new(server.url())
            .unwrap()
            .with_auth("test".to_string(), "pass".to_string());
        
        let criteria = SearchCriteria {
            query: None,
            categories: vec![],
            tag: None,
        };
        let page_request = PageRequest {
            cursor: None,
            size: Some(20),
            direction: None,
        };

        let result = client.list_entries(&criteria, &page_request).await;
        if let Err(ref e) = result {
            eprintln!("Authentication test failed with error: {:?}", e);
        }
        assert!(result.is_ok());

        // mock.assert_async().await;
    }
}