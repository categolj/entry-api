CREATE UNIQUE INDEX IF NOT EXISTS entry_unique_key ON entry(public_entry_id, tenant_id);
CREATE INDEX IF NOT EXISTS entry_last_modified_date ON entry(last_modified_date);
CREATE INDEX IF NOT EXISTS entry_categories_entry_id_idx ON entry_categories(entry_id);
CREATE INDEX IF NOT EXISTS entry_categories_name_idx ON entry_categories(name);
CREATE UNIQUE INDEX IF NOT EXISTS entry_categories_unique_key ON entry_categories(entry_id, position);
CREATE INDEX IF NOT EXISTS entry_tags_entry_id_idx ON entry_tags(entry_id);
CREATE INDEX IF NOT EXISTS entry_tags_name_idx ON entry_tags(name);
CREATE INDEX IF NOT EXISTS entry_tokens_token_idx ON entry_tokens(token);
-- foreign keys only for postgresql
ALTER TABLE entry_categories
ADD CONSTRAINT fk_entry_categories_entry_id FOREIGN KEY (entry_id) REFERENCES entry(id);
ALTER TABLE entry_tags
ADD CONSTRAINT fk_entry_tags_entry_id FOREIGN KEY (entry_id) REFERENCES entry(id);
ALTER TABLE entry_tokens
ADD CONSTRAINT fk_entry_tokens_entry_id FOREIGN KEY (entry_id) REFERENCES entry(id);