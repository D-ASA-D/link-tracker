CREATE TABLE IF NOT EXISTS chats (
    id BIGINT PRIMARY KEY
);

CREATE TABLE IF NOT EXISTS links (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    last_updated TIMESTAMPTZ,
    last_checked_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    chat_id BIGINT NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
    link_id BIGINT NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_subscription UNIQUE (chat_id, link_id)
);

CREATE TABLE IF NOT EXISTS tags (
    id BIGSERIAL PRIMARY KEY,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS subscription_tags (
    subscription_id BIGINT NOT NULL REFERENCES subscriptions(id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    PRIMARY KEY (subscription_id, tag_id)
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_chat_id ON subscriptions(chat_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_link_id ON subscriptions(link_id);
CREATE INDEX IF NOT EXISTS idx_links_last_updated ON links(last_updated);
CREATE INDEX IF NOT EXISTS idx_links_last_checked_at ON links(last_checked_at);
CREATE INDEX IF NOT EXISTS idx_links_last_checked_at_id ON links(last_checked_at, id);
