CREATE UNIQUE INDEX ASYNC IF NOT EXISTS entry_unique_key ON entry(public_entry_id, tenant_id);
CREATE INDEX ASYNC IF NOT EXISTS entry_last_modified_date ON entry(last_modified_date);
CREATE INDEX ASYNC IF NOT EXISTS entry_categories_entry_id_idx ON entry_categories(entry_id);
CREATE INDEX ASYNC IF NOT EXISTS entry_categories_name_idx ON entry_categories(name);
CREATE UNIQUE INDEX ASYNC IF NOT EXISTS entry_categories_unique_key ON entry_categories(entry_id, position);
CREATE INDEX ASYNC IF NOT EXISTS entry_tags_entry_id_idx ON entry_tags(entry_id);
CREATE INDEX ASYNC IF NOT EXISTS entry_tags_name_idx ON entry_tags(name);
CREATE INDEX ASYNC IF NOT EXISTS entry_tokens_token_idx ON entry_tokens(token);
