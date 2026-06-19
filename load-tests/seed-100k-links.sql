INSERT INTO chats (id)
SELECT chat_id
FROM generate_series(900000000, 900000999) AS chat_id
ON CONFLICT DO NOTHING;

WITH inserted_links AS (
    INSERT INTO links (url, last_updated, last_checked_at)
    SELECT
        'https://github.com/D-ASA-D/preloaded-link-' || link_number,
        NOW(),
        NOW()
    FROM generate_series(1, 100000) AS link_number
    ON CONFLICT (url) DO UPDATE SET url = EXCLUDED.url
    RETURNING id, url
),
numbered_links AS (
    SELECT
        id,
        row_number() OVER (ORDER BY id) AS rn
    FROM inserted_links
)
INSERT INTO subscriptions (chat_id, link_id)
SELECT
    900000000 + ((rn - 1) / 100),
    id
FROM numbered_links
ON CONFLICT DO NOTHING;
