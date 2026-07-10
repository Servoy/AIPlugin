-- =====================================================================
-- SQL Researcher - skill pack store
-- ---------------------------------------------------------------------
-- Run this against the dedicated Servoy database server referenced by the
-- `skillServerName` variable in forms/sqlResearcher.js (default: "skills").
--
-- The SQL Researcher agent:
--   * sees an INDEX of enabled packs (name + description) on every run
--   * calls loadSkill(name) to read a pack's full `content` on demand
--   * calls saveLearning(name, description, content) to write back durable,
--     reusable facts it discovers (stored as kind = 'learning')
--
-- DDL below is PostgreSQL. Adjust types for another product if needed
-- (SERIAL -> IDENTITY/auto-increment, TEXT -> CLOB/VARCHAR(max),
--  BOOLEAN -> equivalent, TIMESTAMP -> DATETIME, now() -> CURRENT_TIMESTAMP).
-- =====================================================================

CREATE TABLE skill_packs (
    skill_id     SERIAL PRIMARY KEY,               -- Servoy primary key
    name         VARCHAR(100) NOT NULL,            -- slug used by loadSkill / saveLearning
    description  VARCHAR(500) NOT NULL,            -- one-liner shown in the always-on index (Tier 0)
    content      TEXT,                             -- full markdown body, loaded on demand (Tier 1)
    kind         VARCHAR(20)  NOT NULL DEFAULT 'pack',  -- 'pack' (human, authoritative) | 'learning' (agent)
    tags         VARCHAR(500),                     -- optional comma-separated topics for relevance
    confidence   INTEGER,                          -- 0-100 for learnings; NULL/100 for curated packs
    source       VARCHAR(200),                     -- 'human' | 'agent:sqlResearcher' | originating question
    enabled      BOOLEAN NOT NULL DEFAULT true,    -- toggle a pack off without deleting it
    usage_count  INTEGER NOT NULL DEFAULT 0,       -- bumped on load - shows which packs earn their keep
    created_at   TIMESTAMP NOT NULL DEFAULT now(),
    modified_at  TIMESTAMP
);

CREATE UNIQUE INDEX skill_packs_name_uq ON skill_packs (name);

-- ---------------------------------------------------------------------
-- Example curated packs (kind = 'pack'). Replace with your own domain knowledge.
-- ---------------------------------------------------------------------

INSERT INTO skill_packs (name, description, content, kind, tags, source) VALUES
('revenue-semantics',
 'How revenue, discounts and refunds map across the orders tables',
 E'# Revenue semantics\n\n'
 || E'- Line revenue = order_details.unitprice * order_details.quantity * (1 - order_details.discount).\n'
 || E'- "Sales" excludes cancelled/refunded orders. Check the order status before summing.\n'
 || E'- Do not use products.unitprice for historical revenue; always use the price captured on order_details.\n'
 || E'- Join path: customers -> orders (customerid) -> order_details (orderid) -> products (productid).\n',
 'pack', 'orders,revenue,products', 'human');

INSERT INTO skill_packs (name, description, content, kind, tags, source) VALUES
('openedge-pub-conventions',
 'Identifier quoting and schema rules for the Progress OpenEdge (PUB) database',
 E'# Progress OpenEdge (PUB) conventions\n\n'
 || E'- All application tables live in the "PUB" schema.\n'
 || E'- Always schema-qualify and double-quote identifiers: SELECT * FROM "PUB"."Customer".\n'
 || E'- Identifiers are case-sensitive; match the exact casing from describeTables.\n'
 || E'- Use FETCH FIRST n ROWS ONLY (not TOP / LIMIT) to cap rows.\n',
 'pack', 'openedge,dialect,quoting', 'human');

-- Agent-written rows are added automatically by saveLearning with kind = 'learning'.
-- To promote a good learning into an authoritative pack, set its kind to 'pack'
-- (and optionally clear confidence): UPDATE skill_packs SET kind = 'pack' WHERE name = '...';
