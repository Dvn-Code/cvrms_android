-- =============================================================================
-- CASAVISTA RESORT & VILLAS - MEDIA ASSETS & LIVE CLOUD ANNOUNCEMENTS
-- Run this in your Supabase SQL Editor
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS resort;

-- 1. Media Assets Table for base64 storage of Room, Court, and Profile photos
CREATE TABLE IF NOT EXISTS resort.media_asset (
    entity_id VARCHAR(120) PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL DEFAULT 'facility',
    image_base64 TEXT NOT NULL DEFAULT '',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Ensure all columns exist even if the table already existed previously
ALTER TABLE resort.media_asset ADD COLUMN IF NOT EXISTS entity_type VARCHAR(50) DEFAULT 'facility';
ALTER TABLE resort.media_asset ADD COLUMN IF NOT EXISTS image_base64 TEXT;
ALTER TABLE resort.media_asset ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_media_asset_type ON resort.media_asset(entity_type);

-- 2. Announcements Table for live broadcasting across guest and staff devices
CREATE TABLE IF NOT EXISTS resort.announcement (
    announcement_id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL DEFAULT '',
    image_base64 TEXT,
    author_name VARCHAR(100) DEFAULT 'Super Admin',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Crucial: Ensure columns exist on already-existing table before creating index or querying
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS title VARCHAR(255);
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS content TEXT DEFAULT '';
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS image_base64 TEXT;
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS author_name VARCHAR(100) DEFAULT 'Super Admin';
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE resort.announcement ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- If existing table had 'body' column, migrate text into 'content'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'resort' AND table_name = 'announcement' AND column_name = 'body'
    ) THEN
        UPDATE resort.announcement SET content = body WHERE (content IS NULL OR content = '') AND body IS NOT NULL;
    END IF;
END $$;

-- Fix any NULLs in boolean/timestamp fields
UPDATE resort.announcement SET is_active = TRUE WHERE is_active IS NULL;
UPDATE resort.announcement SET author_name = 'Super Admin' WHERE author_name IS NULL;
UPDATE resort.announcement SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE resort.announcement SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

-- 3. Create index now that is_active is guaranteed to exist
CREATE INDEX IF NOT EXISTS idx_announcement_created_at 
ON resort.announcement(created_at DESC) 
WHERE is_active = TRUE;

-- 4. Insert welcome announcement if table is empty
INSERT INTO resort.announcement (announcement_id, title, content, author_name, created_at, is_active)
SELECT 'ANN-0000001', 'Welcome to Casa Vista Resort!', 'Experience luxury seaside villas and professional pickleball & basketball courts. Reserve your stay or book your court directly in our mobile app.', 'Super Admin', NOW(), TRUE
WHERE NOT EXISTS (SELECT 1 FROM resort.announcement WHERE announcement_id = 'ANN-0000001');
