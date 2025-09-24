-- Aurora DSQL supports JSON and JSONB as runtime data types for query processing, though these cannot be used as column data types in table schemas.
CREATE TABLE IF NOT EXISTS entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    public_entry_id BIGINT NOT NULL,
    title VARCHAR(512) NOT NULL,
    summary TEXT NOT NULL DEFAULT '',
    content TEXT NOT NULL,
    created_by VARCHAR(128) NOT NULL default 'system',
    created_date TIMESTAMP WITH TIME ZONE NOT NULL default CURRENT_TIMESTAMP,
    last_modified_by VARCHAR(128) NOT NULL default 'system',
    last_modified_date TIMESTAMP WITH TIME ZONE NOT NULL default CURRENT_TIMESTAMP,
    tenant_id VARCHAR(128) NOT NULL DEFAULT '_'::VARCHAR,
    categories TEXT NOT NULL DEFAULT '[]',
    tags TEXT NOT NULL DEFAULT '[]'
);;

-- Create Inverted indexes tables because Aurora DSQL does not support GIN indexes
CREATE TABLE IF NOT EXISTS entry_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    position INTEGER NOT NULL default 1
);;

CREATE TABLE IF NOT EXISTS entry_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entry_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    version VARCHAR(255)
);;

-- Create the tokens table for full-text search because Aurora DSQL does not support pg_trgm extension
CREATE TABLE IF NOT EXISTS entry_tokens (
    entry_id UUID NOT NULL,
    token VARCHAR(255) NOT NULL,
    PRIMARY KEY (entry_id, token)
);;
