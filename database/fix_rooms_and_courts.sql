-- =============================================================================
-- CASA VISTA RESORT MANAGEMENT SYSTEM (CVRMS)
-- SUPABASE POSTGRESQL SCHEMA REPAIR SCRIPT FOR `resort.room` & FACILITIES
-- Run this in Supabase Dashboard -> SQL Editor -> Run
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS resort;
CREATE SCHEMA IF NOT EXISTS pos;

-- 1. Ensure `resort.room` table exists with complete columns and constraints
CREATE TABLE IF NOT EXISTS resort.room (
    room_id character varying(10) NOT NULL,
    room_number character varying(10) NOT NULL,
    rate_per_day numeric(10,2) NOT NULL,
    max_pax integer NOT NULL,
    status character varying(20) DEFAULT 'Ready' NOT NULL,
    room_type character varying(20) DEFAULT 'Standard' NOT NULL,
    CONSTRAINT pk_room PRIMARY KEY (room_id),
    CONSTRAINT uq_room_number UNIQUE (room_number),
    CONSTRAINT chk_room_max_pax CHECK ((max_pax > 0)),
    CONSTRAINT chk_room_rate_per_day CHECK ((rate_per_day >= (0)::numeric)),
    CONSTRAINT chk_room_status CHECK (((status)::text = ANY ((ARRAY['Ready'::character varying, 'Occupied'::character varying, 'Cleaning'::character varying, 'Maintenance'::character varying])::text[]))),
    CONSTRAINT chk_room_type CHECK (((room_type)::text = ANY ((ARRAY['Standard'::character varying, 'Deluxe'::character varying])::text[])))
);

-- Enable RLS and add public/service access policy
ALTER TABLE resort.room ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE schemaname = 'resort' AND tablename = 'room' AND policyname = 'Allow public read access to rooms'
    ) THEN
        CREATE POLICY "Allow public read access to rooms" ON resort.room FOR SELECT USING (true);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE schemaname = 'resort' AND tablename = 'room' AND policyname = 'Allow all access to rooms'
    ) THEN
        CREATE POLICY "Allow all access to rooms" ON resort.room FOR ALL USING (true) WITH CHECK (true);
    END IF;
END $$;

-- 2. Restore/Upsert Default Villa Suite Details (Room Data)
INSERT INTO resort.room (room_id, room_number, rate_per_day, max_pax, status, room_type)
VALUES 
    ('RM-101', '101', 2500.00, 4, 'Ready', 'Standard'),
    ('RM-102', '102', 2500.00, 4, 'Ready', 'Standard'),
    ('RM-103', '103', 3500.00, 6, 'Ready', 'Deluxe')
ON CONFLICT (room_id) DO UPDATE SET
    room_number  = EXCLUDED.room_number,
    rate_per_day = EXCLUDED.rate_per_day,
    max_pax      = EXCLUDED.max_pax,
    status       = EXCLUDED.status,
    room_type    = EXCLUDED.room_type;

-- 3. Ensure `resort.court` table and records exist
CREATE TABLE IF NOT EXISTS resort.court (
    court_id character varying(10) NOT NULL,
    court_name character varying(50) NOT NULL,
    court_type character varying(30) NOT NULL,
    rate_daytime numeric(10,2) NOT NULL,
    rate_nighttime numeric(10,2) NOT NULL,
    paddle_rate numeric(10,2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT pk_court PRIMARY KEY (court_id),
    CONSTRAINT chk_court_paddle_rate CHECK ((paddle_rate >= (0)::numeric)),
    CONSTRAINT chk_court_rate_daytime CHECK ((rate_daytime >= (0)::numeric)),
    CONSTRAINT chk_court_rate_nighttime CHECK ((rate_nighttime >= (0)::numeric)),
    CONSTRAINT chk_court_type CHECK (((court_type)::text = ANY ((ARRAY['Pickleball'::character varying, 'Basketball'::character varying])::text[])))
);

ALTER TABLE resort.court ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies WHERE schemaname = 'resort' AND tablename = 'court' AND policyname = 'Allow all access to courts'
    ) THEN
        CREATE POLICY "Allow all access to courts" ON resort.court FOR ALL USING (true) WITH CHECK (true);
    END IF;
END $$;

INSERT INTO resort.court (court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate)
VALUES 
    ('CRT-01', 'Pickleball Court 1', 'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-02', 'Pickleball Court 2', 'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-PB1', 'Pickleball Court 1', 'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-PB2', 'Pickleball Court 2', 'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-PB3', 'Pickleball Court 3', 'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-BK1', 'Basketball Half-Court', 'Basketball', 500.00, 750.00, 0.00)
ON CONFLICT (court_id) DO UPDATE SET
    court_name     = EXCLUDED.court_name,
    court_type     = EXCLUDED.court_type,
    rate_daytime   = EXCLUDED.rate_daytime,
    rate_nighttime = EXCLUDED.rate_nighttime,
    paddle_rate    = EXCLUDED.paddle_rate;

-- 4. Ensure `resort.booking` has `court_date` column and proper default
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'resort' AND table_name = 'booking' AND column_name = 'court_date'
    ) THEN
        ALTER TABLE resort.booking ADD COLUMN court_date date;
    END IF;
END $$;

-- Update existing court bookings so court_date is populated if it was null
UPDATE resort.booking 
SET court_date = booking_date 
WHERE court_id IS NOT NULL AND court_date IS NULL;

-- 5. Ensure `resort.booking_room` has `booked_rate`
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_schema = 'resort' AND table_name = 'booking_room' AND column_name = 'booked_rate'
    ) THEN
        IF EXISTS (
            SELECT 1 FROM information_schema.columns 
            WHERE table_schema = 'resort' AND table_name = 'booking_room' AND column_name = 'rate_per_day'
        ) THEN
            ALTER TABLE resort.booking_room RENAME COLUMN rate_per_day TO booked_rate;
        ELSE
            ALTER TABLE resort.booking_room ADD COLUMN booked_rate numeric(10,2) DEFAULT 2500.00 NOT NULL;
        END IF;
    END IF;
END $$;

-- Verify table outputs
SELECT 'resort.room restored successfully' AS status, count(*) AS total_rooms FROM resort.room;
SELECT 'resort.court restored successfully' AS status, count(*) AS total_courts FROM resort.court;
