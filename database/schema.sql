--
-- Casa Vista Resort Management System (CVRMS)
-- Official PostgreSQL / Supabase Database Dump & Schema
--

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- SCHEMAS: pos and resort
--

CREATE SCHEMA IF NOT EXISTS pos;
CREATE SCHEMA IF NOT EXISTS resort;

--
-- FUNCTION: fn_check_exclusive_collision()
--

CREATE OR REPLACE FUNCTION resort.fn_check_exclusive_collision() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
BEGIN
  IF NEW.status = 'Cancelled' THEN
    RETURN NEW;
  END IF;

  PERFORM pg_advisory_xact_lock(hashtext('cvrms_exclusive'));

  IF NEW.exclusive THEN
    IF EXISTS (
      SELECT 1 FROM resort.booking_room br
      WHERE br.is_active
        AND br.stay && daterange(NEW.check_in_date, NEW.check_out_date, '[)')
    ) THEN
      RAISE EXCEPTION 'Exclusive booking blocked: One or more rooms are already reserved in this date range.';
    END IF;

    IF EXISTS (
      SELECT 1 FROM resort.booking b
      WHERE b.court_id IS NOT NULL
        AND b.status <> 'Cancelled'
        AND b.booking_date >= NEW.check_in_date
        AND b.booking_date <  NEW.check_out_date
    ) THEN
      RAISE EXCEPTION 'Exclusive booking blocked: One or more courts are already reserved in this date range.';
    END IF;

  ELSIF NEW.court_id IS NOT NULL THEN
    IF EXISTS (
      SELECT 1 FROM resort.booking b
      WHERE b.exclusive
        AND b.status <> 'Cancelled'
        AND NEW.booking_date >= b.check_in_date
        AND NEW.booking_date <  b.check_out_date
    ) THEN
      RAISE EXCEPTION 'Court booking blocked: An Exclusive Whole-Resort booking is active on that date.';
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

--
-- FUNCTION: fn_enforce_br1_room_caps()
--

CREATE OR REPLACE FUNCTION resort.fn_enforce_br1_room_caps() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
DECLARE
  v_current_count INT;
  v_target_room_type VARCHAR;
BEGIN
  PERFORM pg_advisory_xact_lock(hashtext('cvrms_exclusive'));

  SELECT room_type INTO v_target_room_type
  FROM resort.room WHERE room_id = NEW.room_id;

  SELECT COUNT(*) INTO v_current_count
  FROM resort.booking_room br
  JOIN resort.room r ON br.room_id = r.room_id
  WHERE br.booking_id = NEW.booking_id
    AND br.room_id <> NEW.room_id
    AND r.room_type = v_target_room_type;

  IF v_target_room_type = 'Standard' AND v_current_count >= 3 THEN
    RAISE EXCEPTION 'BR-1 Violation: A single booking cannot reserve more than 3 standard rooms.';
  ELSIF v_target_room_type = 'Deluxe' AND v_current_count >= 1 THEN
    RAISE EXCEPTION 'BR-1 Violation: A single booking cannot reserve more than 1 deluxe room.';
  END IF;

  RETURN NEW;
END;
$$;

--
-- FUNCTION: fn_propagate_booking_to_rooms()
--

CREATE OR REPLACE FUNCTION resort.fn_propagate_booking_to_rooms() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
BEGIN
  UPDATE resort.booking_room
  SET is_active = (NEW.status <> 'Cancelled'),
      stay = daterange(NEW.check_in_date, NEW.check_out_date, '[)')
  WHERE booking_id = NEW.booking_id;
  RETURN NEW;
END;
$$;

--
-- FUNCTION: fn_require_rooms()
--

CREATE OR REPLACE FUNCTION resort.fn_require_rooms() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
BEGIN
  IF NEW.court_id IS NULL AND NOT NEW.exclusive AND NEW.status <> 'Cancelled'
     AND NOT EXISTS (SELECT 1 FROM resort.booking_room WHERE booking_id = NEW.booking_id) THEN
    RAISE EXCEPTION 'BR-1 Violation: Booking % has no rooms assigned.', NEW.booking_id;
  END IF;
  RETURN NULL;
END;
$$;

--
-- FUNCTION: fn_require_rooms_after_delete()
--

CREATE OR REPLACE FUNCTION resort.fn_require_rooms_after_delete() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM resort.booking b
    WHERE b.booking_id = OLD.booking_id
      AND b.court_id IS NULL
      AND NOT b.exclusive
      AND b.status <> 'Cancelled'
  )
  AND NOT EXISTS (
    SELECT 1 FROM resort.booking_room
    WHERE booking_id = OLD.booking_id
  ) THEN
    RAISE EXCEPTION 'BR-1 Violation: Booking % would be left with no rooms.', OLD.booking_id;
  END IF;
  RETURN NULL;
END;
$$;

--
-- FUNCTION: fn_sync_booking_room_stay()
--

CREATE OR REPLACE FUNCTION resort.fn_sync_booking_room_stay() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
DECLARE
  v_in DATE;
  v_out DATE;
  v_status VARCHAR;
BEGIN
  PERFORM pg_advisory_xact_lock(hashtext('cvrms_exclusive'));

  SELECT check_in_date, check_out_date, status INTO v_in, v_out, v_status
  FROM resort.booking
  WHERE booking_id = NEW.booking_id;

  IF v_in IS NULL OR v_out IS NULL THEN
    RAISE EXCEPTION 'Cannot assign room: Parent booking missing check-in or check-out dates.';
  END IF;

  IF EXISTS (
    SELECT 1 FROM resort.booking b
    WHERE b.exclusive
      AND b.status <> 'Cancelled'
      AND daterange(b.check_in_date, b.check_out_date, '[)') && daterange(v_in, v_out, '[)')
  ) THEN
    RAISE EXCEPTION 'Cannot reserve individual room: An Exclusive Whole-Resort booking is active for these dates.';
  END IF;

  NEW.stay := daterange(v_in, v_out, '[)');
  NEW.is_active := (v_status <> 'Cancelled');
  RETURN NEW;
END;
$$;

SET default_tablespace = '';
SET default_table_access_method = heap;

--
-- TABLES: POS & RESORT
--

CREATE TABLE IF NOT EXISTS pos.pos_item (
    item_id character varying(10) NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(50) NOT NULL,
    price numeric(10,2) NOT NULL,
    CONSTRAINT chk_pos_item_price CHECK ((price >= (0)::numeric)),
    CONSTRAINT pk_pos_item PRIMARY KEY (item_id)
);

CREATE TABLE IF NOT EXISTS pos.pos_order (
    order_id character varying(15) NOT NULL,
    customer_id character varying(10),
    staff_id character varying(10) NOT NULL,
    order_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    total_amount numeric(10,2) DEFAULT 0.00 NOT NULL,
    booking_id character varying(15),
    CONSTRAINT chk_pos_order_total_amount CHECK ((total_amount >= (0)::numeric)),
    CONSTRAINT pk_pos_order PRIMARY KEY (order_id)
);

CREATE TABLE IF NOT EXISTS pos.pos_order_item (
    order_id character varying(15) NOT NULL,
    item_id character varying(10) NOT NULL,
    quantity integer NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    CONSTRAINT chk_pos_order_item_quantity CHECK ((quantity > 0)),
    CONSTRAINT chk_pos_order_item_subtotal CHECK ((subtotal >= (0)::numeric)),
    CONSTRAINT pk_pos_order_item PRIMARY KEY (order_id, item_id)
);

CREATE TABLE IF NOT EXISTS resort.court (
    court_id character varying(10) NOT NULL,
    court_name character varying(50) NOT NULL,
    court_type character varying(30) NOT NULL,
    rate_daytime numeric(10,2) NOT NULL,
    rate_nighttime numeric(10,2) NOT NULL,
    paddle_rate numeric(10,2) NOT NULL,
    CONSTRAINT chk_court_paddle_rate CHECK ((paddle_rate >= (0)::numeric)),
    CONSTRAINT chk_court_rate_daytime CHECK ((rate_daytime >= (0)::numeric)),
    CONSTRAINT chk_court_rate_nighttime CHECK ((rate_nighttime >= (0)::numeric)),
    CONSTRAINT chk_court_type CHECK (((court_type)::text = ANY ((ARRAY['Pickleball'::character varying, 'Basketball'::character varying])::text[]))),
    CONSTRAINT pk_court PRIMARY KEY (court_id)
);

CREATE SEQUENCE IF NOT EXISTS resort.customer_id_seq
    START WITH 102
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE IF NOT EXISTS resort.customer (
    customer_id character varying(10) DEFAULT ('CUST-'::text || lpad((nextval('resort.customer_id_seq'::regclass))::text, 3, '0'::text)) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    email character varying(120),
    phone character varying(20) NOT NULL,
    address character varying(255),
    birthdate character varying(30),
    gender character varying(30),
    customer_type character varying(20) DEFAULT 'Registered'::character varying NOT NULL,
    middle_name character varying(50),
    CONSTRAINT chk_customer_profile_completeness CHECK ((((customer_type)::text = 'Walk-In'::text) OR ((email IS NOT NULL) AND (phone IS NOT NULL) AND (address IS NOT NULL)))),
    CONSTRAINT chk_customer_type CHECK (((customer_type)::text = ANY ((ARRAY['Registered'::character varying, 'Walk-In'::character varying])::text[]))),
    CONSTRAINT pk_customer PRIMARY KEY (customer_id),
    CONSTRAINT uq_customer_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS resort.room (
    room_id character varying(10) NOT NULL,
    room_number character varying(10) NOT NULL,
    rate_per_day numeric(10,2) NOT NULL,
    max_pax integer NOT NULL,
    status character varying(20) NOT NULL,
    room_type character varying(20) DEFAULT 'Standard'::character varying NOT NULL,
    CONSTRAINT chk_room_max_pax CHECK ((max_pax > 0)),
    CONSTRAINT chk_room_rate_per_day CHECK ((rate_per_day >= (0)::numeric)),
    CONSTRAINT chk_room_status CHECK (((status)::text = ANY ((ARRAY['Ready'::character varying, 'Occupied'::character varying, 'Cleaning'::character varying, 'Maintenance'::character varying])::text[]))),
    CONSTRAINT chk_room_type CHECK (((room_type)::text = ANY ((ARRAY['Standard'::character varying, 'Deluxe'::character varying])::text[]))),
    CONSTRAINT pk_room PRIMARY KEY (room_id),
    CONSTRAINT uq_room_number UNIQUE (room_number)
);

CREATE TABLE IF NOT EXISTS resort.staff (
    staff_id character varying(10) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    role character varying(50) NOT NULL,
    middle_name character varying(50),
    CONSTRAINT chk_staff_role CHECK (((role)::text = ANY ((ARRAY['Front Desk'::character varying, 'Cashier'::character varying, 'Manager'::character varying, 'Admin'::character varying])::text[]))),
    CONSTRAINT pk_staff PRIMARY KEY (staff_id)
);

CREATE TABLE IF NOT EXISTS resort.booking (
    booking_id character varying(15) NOT NULL,
    customer_id character varying(10) NOT NULL,
    staff_id character varying(10),
    court_id character varying(10),
    booking_date date DEFAULT CURRENT_DATE NOT NULL,
    start_time time without time zone,
    end_time time without time zone,
    check_in_date date,
    check_out_date date,
    exclusive boolean DEFAULT false NOT NULL,
    paddle_count integer DEFAULT 0 NOT NULL,
    status character varying(20) DEFAULT 'Pending'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    hold_expires_at timestamp with time zone,
    total_amount numeric(10,2) DEFAULT 0.00 NOT NULL,
    CONSTRAINT chk_booking_court_times CHECK (((start_time IS NULL) OR (end_time IS NULL) OR (end_time > start_time))),
    CONSTRAINT chk_booking_dates CHECK (((check_in_date IS NULL) OR (check_out_date > check_in_date))),
    CONSTRAINT chk_booking_paddle_count CHECK ((paddle_count >= 0)),
    CONSTRAINT chk_booking_shape CHECK ((((court_id IS NOT NULL) AND (exclusive = false) AND (start_time IS NOT NULL) AND (end_time IS NOT NULL) AND (check_in_date IS NULL) AND (check_out_date IS NULL)) OR ((court_id IS NULL) AND (start_time IS NULL) AND (end_time IS NULL) AND (check_in_date IS NOT NULL) AND (check_out_date IS NOT NULL) AND (paddle_count = 0)))),
    CONSTRAINT chk_booking_status CHECK (((status)::text = ANY ((ARRAY['Pending'::character varying, 'Confirmed'::character varying, 'Checked-In'::character varying, 'Completed'::character varying, 'Cancelled'::character varying])::text[]))),
    CONSTRAINT chk_booking_total_amount CHECK ((total_amount >= 0.00)),
    CONSTRAINT pk_booking PRIMARY KEY (booking_id)
);

CREATE TABLE IF NOT EXISTS resort.booking_room (
    booking_id character varying(15) NOT NULL,
    room_id character varying(10) NOT NULL,
    rate_per_day numeric(10,2) NOT NULL,
    stay daterange NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT booking_room_rate_per_day_check CHECK ((rate_per_day >= (0)::numeric)),
    CONSTRAINT booking_room_pkey PRIMARY KEY (booking_id, room_id)
);

CREATE TABLE IF NOT EXISTS resort.payment (
    payment_id character varying(15) NOT NULL,
    booking_id character varying(15),
    order_id character varying(15),
    amount numeric(10,2) NOT NULL,
    method character varying(30) NOT NULL,
    payment_type character varying(20) NOT NULL,
    paid_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT chk_payment_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT chk_payment_exclusive_arc CHECK ((((booking_id IS NOT NULL) AND (order_id IS NULL)) OR ((booking_id IS NULL) AND (order_id IS NOT NULL)))),
    CONSTRAINT chk_payment_method CHECK (((method)::text = ANY ((ARRAY['Cash'::character varying, 'Card'::character varying, 'GCash'::character varying, 'Maya'::character varying, 'Online Gateway'::character varying])::text[]))),
    CONSTRAINT chk_payment_type CHECK (((payment_type)::text = ANY ((ARRAY['Payment'::character varying, 'Refund'::character varying])::text[]))),
    CONSTRAINT pk_payment PRIMARY KEY (payment_id)
);

--
-- FOREIGN KEYS
--

ALTER TABLE pos.pos_order
    ADD CONSTRAINT fk_pos_order_booking FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_pos_order_customer FOREIGN KEY (customer_id) REFERENCES resort.customer(customer_id) ON UPDATE CASCADE ON DELETE SET NULL,
    ADD CONSTRAINT fk_pos_order_staff FOREIGN KEY (staff_id) REFERENCES resort.staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE pos.pos_order_item
    ADD CONSTRAINT fk_pos_order_item_item FOREIGN KEY (item_id) REFERENCES pos.pos_item(item_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_pos_order_item_order FOREIGN KEY (order_id) REFERENCES pos.pos_order(order_id) ON UPDATE CASCADE ON DELETE CASCADE;

ALTER TABLE resort.booking_room
    ADD CONSTRAINT booking_room_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE CASCADE,
    ADD CONSTRAINT booking_room_room_id_fkey FOREIGN KEY (room_id) REFERENCES resort.room(room_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE resort.booking
    ADD CONSTRAINT fk_booking_court FOREIGN KEY (court_id) REFERENCES resort.court(court_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES resort.customer(customer_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_booking_staff FOREIGN KEY (staff_id) REFERENCES resort.staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE resort.payment
    ADD CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE RESTRICT,
    ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES pos.pos_order(order_id) ON UPDATE CASCADE ON DELETE RESTRICT;

--
-- ROW LEVEL SECURITY
--

ALTER TABLE pos.pos_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE pos.pos_order ENABLE ROW LEVEL SECURITY;
ALTER TABLE pos.pos_order_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.booking ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.court ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.customer ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.payment ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.room ENABLE ROW LEVEL SECURITY;
ALTER TABLE resort.staff ENABLE ROW LEVEL SECURITY;

--
-- SAMPLE SEED DATA FOR TESTING & EVALUATION
--

INSERT INTO resort.staff (staff_id, first_name, last_name, role) VALUES
('STF-001', 'Carlos', 'Reyes', 'Front Desk'),
('STF-002', 'Maria', 'Santos', 'Manager'),
('STF-003', 'John', 'Doe', 'Cashier')
ON CONFLICT (staff_id) DO NOTHING;

INSERT INTO resort.court (court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate) VALUES
('CRT-PB1', 'Pickleball Court 1', 'Pickleball', 400.00, 600.00, 50.00),
('CRT-PB2', 'Pickleball Court 2', 'Pickleball', 400.00, 600.00, 50.00),
('CRT-BK1', 'Basketball Half-Court', 'Basketball', 500.00, 750.00, 0.00)
ON CONFLICT (court_id) DO NOTHING;

INSERT INTO resort.room (room_id, room_number, rate_per_day, max_pax, status, room_type) VALUES
('RM-101', '101', 2500.00, 4, 'Ready', 'Standard'),
('RM-102', '102', 2500.00, 4, 'Ready', 'Standard'),
('RM-103', '103', 3500.00, 6, 'Ready', 'Deluxe')
ON CONFLICT (room_id) DO NOTHING;

INSERT INTO resort.customer (customer_id, first_name, last_name, email, phone, address, customer_type) VALUES
('CUST-101', 'Alex', 'Morgan', 'alex.morgan@casavista.com', '09123456789', 'Cebu City, Philippines', 'Registered'),
('CUST-102', 'Guest', 'Walk-In', 'walkin@casavista.com', '09000000000', 'Cebu City, Philippines', 'Walk-In')
ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO pos.pos_item (item_id, name, category, price) VALUES
('ITM-001', 'Bottled Mineral Water 500ml', 'Beverages', 35.00),
('ITM-002', 'Pickleball Paddle Rental', 'Equipment', 50.00),
('ITM-003', 'Resort Towel Rental', 'Amenities', 100.00)
ON CONFLICT (item_id) DO NOTHING;
