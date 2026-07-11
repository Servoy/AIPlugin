-- =====================================================================
-- SQL Researcher - agent run log
-- ---------------------------------------------------------------------
-- Run this against the SAME Servoy server as skill_packs (the one named by
-- the `skillServerName` variable in forms/sqlResearcher.js).
--
-- One row is written per research run (on success AND on failure), capturing
-- the full trace: prompt, queries, reasoning narration, skills used/created/
-- updated, errors, and the final summary. This is the backbone for auditing,
-- debugging, analytics, and reviewing agent-created learnings before trusting
-- them.
--
-- DDL is PostgreSQL. Adjust types for another product if needed
-- (SERIAL -> IDENTITY/auto-increment, TEXT -> CLOB/VARCHAR(max),
--  TIMESTAMP -> DATETIME, now() -> CURRENT_TIMESTAMP).
--
-- The list-valued columns (queries, skills_*, errors) are stored as JSON text.
-- =====================================================================

CREATE TABLE agent_run (
    run_id          SERIAL PRIMARY KEY,              -- Servoy primary key
    created_at      TIMESTAMP NOT NULL DEFAULT now(),-- when the run started
    finished_at     TIMESTAMP,                       -- when it finished
    duration_ms     INTEGER,                         -- wall-clock duration
    model           VARCHAR(200),                    -- model used (via the router)
    server_name     VARCHAR(200),                    -- the researched database server
    skill_server    VARCHAR(200),                    -- the skill/log database server
    user_prompt     TEXT,                            -- the research question
    reasoning_trace TEXT,                            -- the agent's working notes (before ===REPORT===)
    final_summary   TEXT,                            -- the final markdown report
    query_count     INTEGER,                         -- number of runSQL calls
    queries         TEXT,                            -- JSON: [{seq, description, sql, rowCount, error}]
    skills_used     TEXT,                            -- JSON: ["pack-name", ...] loaded this run
    skills_created  TEXT,                            -- JSON: learnings the agent created
    skills_updated  TEXT,                            -- JSON: learnings the agent refined
    errors          TEXT,                            -- JSON: ["Query 3: ERROR ...", ...]
    status          VARCHAR(20),                     -- 'success' | 'error'
    total_tokens    INTEGER                          -- total tokens for the research call
);

-- Handy for a run-history grid (most recent first).
CREATE INDEX agent_run_created_idx ON agent_run (created_at DESC);
