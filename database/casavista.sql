-- Casa Vista Resort Management System (CVRMS)
-- Database Schema and Sample Data Script

-- Drop existing tables if they exist
DROP TABLE IF EXISTS history_logs CASCADE;
DROP TABLE IF EXISTS announcements CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS catalog_items CASCADE;
DROP TABLE IF EXISTS employees CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- 1. USERS / CUSTOMERS TABLE
CREATE TABLE users (
    id VARCHAR(100) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(50) DEFAULT '09000000000',
    address TEXT DEFAULT 'Cebu City, Philippines',
    role VARCHAR(50) DEFAULT 'customer', -- 'customer', 'admin', 'super_admin'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. CATALOG ITEMS / FACILITIES TABLE
CREATE TABLE catalog_items (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100) NOT NULL, -- 'Room', 'Buyout', 'Pickleball', 'Basketball'
    capacity VARCHAR(100),
    price_rate VARCHAR(100),
    badge_tag VARCHAR(100),
    status VARCHAR(50) DEFAULT 'Available', -- 'Available', 'Unavailable', 'Maintenance'
    image_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. BOOKINGS / RESERVATIONS TABLE
CREATE TABLE bookings (
    id VARCHAR(100) PRIMARY KEY,
    customer_id VARCHAR(100) REFERENCES users(id) ON DELETE SET NULL,
    customer_name VARCHAR(255) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    facility_title VARCHAR(255) NOT NULL,
    booking_date DATE NOT NULL,
    time_slot VARCHAR(100) DEFAULT 'All Day',
    total_price VARCHAR(100) NOT NULL,
    status VARCHAR(50) DEFAULT 'Pending', -- 'Pending', 'Confirmed', 'Completed', 'Cancelled'
    payment_method VARCHAR(100) DEFAULT 'Maya',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. ANNOUNCEMENTS TABLE
CREATE TABLE announcements (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author VARCHAR(100) DEFAULT 'Resort Admin',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. EMPLOYEES / STAFF TABLE
CREATE TABLE employees (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'Active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 6. HISTORY / AUDIT LOGS TABLE
CREATE TABLE history_logs (
    id SERIAL PRIMARY KEY,
    action_type VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    performed_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- SAMPLE RECORDS FOR TESTING
-- =========================================================

-- Insert Sample Users
INSERT INTO users (id, email, first_name, last_name, phone, address, role) VALUES
('CUST-1001', 'alex.morgan@casavista.com', 'Alex', 'Morgan', '09123456789', 'Cebu City, Philippines', 'customer'),
('ADMIN-2001', 'admin@casavista.com', 'Resort', 'Admin', '09987654321', 'Mandaue City, Philippines', 'admin'),
('SUPER-3001', 'superadmin@casavista.com', 'Super', 'Administrator', '09112223334', 'Lapu-Lapu City, Philippines', 'super_admin');

-- Insert Sample Catalog Items / Resort Facilities
INSERT INTO catalog_items (title, description, category, capacity, price_rate, badge_tag, status) VALUES
('Standard Villa Suite 101', 'Queen Bed • Mountain View • AC', 'Room', '4 Guests', '₱2,500 / day', 'Standard', 'Available'),
('Standard Villa Suite 102', 'Queen Bed • Poolside Access • AC', 'Room', '4 Guests', '₱2,500 / day', 'Standard', 'Available'),
('Deluxe Family Suite 103', '2 Queen Beds • Balcony • Mini Kitchen', 'Room', '6 Guests', '₱3,500 / day', 'Family Deluxe', 'Available'),
('Whole-Resort Private Buyout', 'Exclusive access to all 3 villas & sports courts', 'Buyout', '14 Max', '₱12,000 / day', 'Exclusive', 'Available'),
('Pickleball Court 1', 'Tournament Hard Court • Outdoor Lighting', 'Pickleball', '2-4 Players', '₱400 - ₱600 / hr', 'Day/Night', 'Available'),
('Pickleball Court 2', 'Tournament Hard Court • Outdoor Lighting', 'Pickleball', '2-4 Players', '₱400 - ₱600 / hr', 'Day/Night', 'Available'),
('Pickleball Court 3', 'Tournament Hard Court • Outdoor Lighting', 'Pickleball', '2-4 Players', '₱400 - ₱600 / hr', 'Day/Night', 'Available'),
('Basketball Half-Court', 'Regulation Pro Flooring • Full Lighting', 'Basketball', '2-10 Players', '₱500 - ₱750 / hr', 'Day/Night', 'Available');

-- Insert Sample Bookings
INSERT INTO bookings (id, customer_id, customer_name, customer_email, facility_title, booking_date, time_slot, total_price, status, payment_method) VALUES
('BOOK-8801', 'CUST-1001', 'Alex Morgan', 'alex.morgan@casavista.com', 'Standard Villa Suite 101', CURRENT_DATE + INTERVAL '1 day', 'All Day', '₱2,500', 'Confirmed', 'Maya'),
('BOOK-8802', 'CUST-1001', 'Alex Morgan', 'alex.morgan@casavista.com', 'Pickleball Court 1', CURRENT_DATE + INTERVAL '2 days', '2:00 PM - 4:00 PM', '₱800', 'Pending', 'Maya');

-- Insert Sample Announcements
INSERT INTO announcements (title, content, author) VALUES
('Welcome to Casa Vista Resort!', 'We are excited to launch our automated booking app. Reserve courts and rooms easily!', 'Super Admin'),
('Night Lighting Upgraded', 'Our Pickleball and Basketball courts now feature LED night lights for evening games.', 'Resort Management');

-- Insert Sample Employees
INSERT INTO employees (name, role, email, status) VALUES
('Carlos Reyes', 'Front Desk Supervisor', 'carlos.reyes@casavista.com', 'Active'),
('Maria Santos', 'Facilities Manager', 'maria.santos@casavista.com', 'Active'),
('John Doe', 'POS Operator', 'john.doe@casavista.com', 'Active');

-- Insert Sample History Logs
INSERT INTO history_logs (action_type, description, performed_by) VALUES
('System Initialization', 'Database schema created and initial sample data inserted.', 'System'),
('Booking Created', 'Reservation BOOK-8801 created for Standard Villa Suite 101.', 'Alex Morgan');
