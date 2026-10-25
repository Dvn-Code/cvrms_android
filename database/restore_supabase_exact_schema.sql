-- =========================================================================
-- CASA VISTA RESORT & VILLAS - EXACT AUDIT-VERIFIED RESTORATION SCRIPT
-- =========================================================================
-- This script matches the EXACT columns in your live Supabase database:
--   resort.room:        (room_id, room_number, rate_per_day, max_pax, status, room_type)
--   resort.court:       (court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate)
--   resort.booking:     (booking_id, customer_id, staff_id, court_id, booking_date, court_date, ...)
--   resort.booking_room:(booking_id, room_id, booked_rate, stay, is_active)
-- =========================================================================

-- 1. Restore/Upsert the 3 Casa Vista Luxury Villas into resort.room:
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

-- 2. Restore/Upsert Sports Courts into resort.court (including both CRT-01..03 and CRT-PB/BK aliases):
INSERT INTO resort.court (court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate)
VALUES
    ('CRT-01',  'Pickleball Court 1',       'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-02',  'Pickleball Court 2',       'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-03',  'Basketball Half-Court',    'Basketball', 500.00, 750.00,   0.00),
    ('CRT-PB1', 'Pickleball Court 1',       'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-PB2', 'Pickleball Court 2',       'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-PB3', 'Pickleball Court 3',       'Pickleball', 400.00, 600.00, 100.00),
    ('CRT-BK1', 'Basketball Half-Court',    'Basketball', 500.00, 750.00,   0.00)
ON CONFLICT (court_id) DO UPDATE SET
    court_name     = EXCLUDED.court_name,
    court_type     = EXCLUDED.court_type,
    rate_daytime   = EXCLUDED.rate_daytime,
    rate_nighttime = EXCLUDED.rate_nighttime,
    paddle_rate    = EXCLUDED.paddle_rate;

-- 3. Verify the restored data:
SELECT room_id, room_number, rate_per_day, max_pax, status, room_type FROM resort.room ORDER BY room_id;
SELECT court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate FROM resort.court ORDER BY court_id;
