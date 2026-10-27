-- =============================================================================
-- CASAVISTA RESORT & VILLAS - MEDIA ASSETS & LIVE CLOUD ANNOUNCEMENTS TABLE
-- Run this script in the Supabase SQL Editor
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS resort;

-- 1. Media Assets Table for base64 storage of Room, Court, Resort, and User Profile pictures
CREATE TABLE IF NOT EXISTS resort.media_asset (
    entity_id VARCHAR(120) PRIMARY KEY, -- e.g. 'RM-101', 'CRT-PB1', 'superadmin@casavista.com', 'guest@gmail.com'
    entity_type VARCHAR(50) NOT NULL,   -- e.g. 'room', 'court', 'profile', 'resort', 'announcement'
    image_base64 TEXT NOT NULL,         -- Base64-encoded image string
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Index for fast lookup by entity_type
CREATE INDEX IF NOT EXISTS idx_media_asset_type ON resort.media_asset(entity_type);

-- 2. Announcements Table for live broadcasting to all guest and staff devices
CREATE TABLE IF NOT EXISTS resort.announcement (
    announcement_id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    image_base64 TEXT,
    author_name VARCHAR(100) DEFAULT 'Super Admin',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    is_active BOOLEAN DEFAULT TRUE
);

-- Index for ordering active announcements
CREATE INDEX IF NOT EXISTS idx_announcement_created_at ON resort.announcement(created_at DESC) WHERE is_active = TRUE;

-- Insert initial welcome announcements if table is empty
INSERT INTO resort.announcement (announcement_id, title, content, author_name, created_at, is_active)
SELECT 'ANN-0000001', 'Welcome to Casa Vista Resort!', 'Experience luxury seaside villas and professional pickleball & basketball courts. Reserve your stay or book your court directly in our mobile app.', 'Super Admin', NOW(), TRUE
WHERE NOT EXISTS (SELECT 1 FROM resort.announcement WHERE announcement_id = 'ANN-0000001');
