--
-- PostgreSQL database dump
--

\restrict ZuH9SaCfQVZQGevR9n8NDWkAKWT2m6cD8DGlConQJN0J9TrDktT4ImIWR3Esuyj

-- Dumped from database version 17.6
-- Dumped by pg_dump version 18.6

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
-- Name: pos; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA pos;


--
-- Name: resort; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA resort;


--
-- Name: fn_check_exclusive_collision(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_check_exclusive_collision() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
BEGIN
  IF NEW.status = 'Cancelled' THEN 
    RETURN NEW; 
  END IF;

  -- Advisory lock serializes concurrent exclusive and unit checks
  PERFORM pg_advisory_xact_lock(hashtext('cvrms_exclusive'));

  IF NEW.exclusive THEN
    -- Check if active rooms are booked during this date range
    IF EXISTS (
      SELECT 1 FROM resort.booking_room br
      WHERE br.is_active
        AND br.stay && daterange(NEW.check_in_date, NEW.check_out_date, '[)')
    ) THEN
      RAISE EXCEPTION 'Exclusive booking blocked: One or more rooms are already reserved in this date range.';
    END IF;

    -- Check if sports courts are booked during this date range
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
    -- If inserting a court booking, ensure no active Exclusive booking covers that date
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
-- Name: fn_enforce_br1_room_caps(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_enforce_br1_room_caps() RETURNS trigger
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
-- Name: fn_propagate_booking_to_rooms(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_propagate_booking_to_rooms() RETURNS trigger
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
-- Name: fn_require_rooms(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_require_rooms() RETURNS trigger
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
-- Name: fn_require_rooms_after_delete(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_require_rooms_after_delete() RETURNS trigger
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
-- Name: fn_sync_booking_room_stay(); Type: FUNCTION; Schema: resort; Owner: -
--

CREATE FUNCTION resort.fn_sync_booking_room_stay() RETURNS trigger
    LANGUAGE plpgsql SECURITY DEFINER
    SET search_path TO 'resort', 'pg_temp'
    AS $$
DECLARE
  v_in DATE;
  v_out DATE;
  v_status VARCHAR;
BEGIN
  -- Shared advisory lock with exclusive check
  PERFORM pg_advisory_xact_lock(hashtext('cvrms_exclusive'));

  SELECT check_in_date, check_out_date, status INTO v_in, v_out, v_status
  FROM resort.booking
  WHERE booking_id = NEW.booking_id;

  IF v_in IS NULL OR v_out IS NULL THEN
    RAISE EXCEPTION 'Cannot assign room: Parent booking missing check-in or check-out dates.';
  END IF;

  -- Ensure no active Exclusive booking covers these dates
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
-- Name: pos_item; Type: TABLE; Schema: pos; Owner: -
--

CREATE TABLE pos.pos_item (
    item_id character varying(10) NOT NULL,
    name character varying(100) NOT NULL,
    category character varying(50) NOT NULL,
    price numeric(10,2) NOT NULL,
    CONSTRAINT chk_pos_item_price CHECK ((price >= (0)::numeric))
);


--
-- Name: pos_order; Type: TABLE; Schema: pos; Owner: -
--

CREATE TABLE pos.pos_order (
    order_id character varying(15) NOT NULL,
    customer_id character varying(10),
    staff_id character varying(10) NOT NULL,
    order_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    total_amount numeric(10,2) DEFAULT 0.00 NOT NULL,
    booking_id character varying(15),
    CONSTRAINT chk_pos_order_total_amount CHECK ((total_amount >= (0)::numeric))
);


--
-- Name: pos_order_item; Type: TABLE; Schema: pos; Owner: -
--

CREATE TABLE pos.pos_order_item (
    order_id character varying(15) NOT NULL,
    item_id character varying(10) NOT NULL,
    quantity integer NOT NULL,
    subtotal numeric(10,2) NOT NULL,
    CONSTRAINT chk_pos_order_item_quantity CHECK ((quantity > 0)),
    CONSTRAINT chk_pos_order_item_subtotal CHECK ((subtotal >= (0)::numeric))
);


--
-- Name: booking; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.booking (
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
    court_date date,
    CONSTRAINT chk_booking_court_times CHECK (((start_time IS NULL) OR (end_time IS NULL) OR (end_time > start_time))),
    CONSTRAINT chk_booking_dates CHECK (((check_in_date IS NULL) OR (check_out_date > check_in_date))),
    CONSTRAINT chk_booking_paddle_count CHECK (((paddle_count >= 0) AND (paddle_count <= 4))),
    CONSTRAINT chk_booking_shape CHECK ((((court_id IS NOT NULL) AND (court_date IS NOT NULL) AND (start_time IS NOT NULL) AND (end_time IS NOT NULL) AND (check_in_date IS NULL) AND (check_out_date IS NULL) AND (exclusive = false)) OR ((court_id IS NULL) AND (court_date IS NULL) AND (start_time IS NULL) AND (end_time IS NULL) AND (check_in_date IS NOT NULL) AND (check_out_date IS NOT NULL) AND (paddle_count = 0)))),
    CONSTRAINT chk_booking_status CHECK (((status)::text = ANY ((ARRAY['Pending'::character varying, 'Confirmed'::character varying, 'Checked-In'::character varying, 'Completed'::character varying, 'Cancelled'::character varying])::text[]))),
    CONSTRAINT chk_booking_total_amount CHECK ((total_amount >= 0.00))
);


--
-- Name: booking_room; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.booking_room (
    booking_id character varying(15) NOT NULL,
    room_id character varying(10) NOT NULL,
    booked_rate numeric(10,2) NOT NULL,
    stay daterange NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT booking_room_rate_per_day_check CHECK ((booked_rate >= (0)::numeric))
);


--
-- Name: court; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.court (
    court_id character varying(10) NOT NULL,
    court_name character varying(50) NOT NULL,
    court_type character varying(30) NOT NULL,
    rate_daytime numeric(10,2) NOT NULL,
    rate_nighttime numeric(10,2) NOT NULL,
    paddle_rate numeric(10,2) NOT NULL,
    CONSTRAINT chk_court_paddle_rate CHECK ((paddle_rate >= (0)::numeric)),
    CONSTRAINT chk_court_rate_daytime CHECK ((rate_daytime >= (0)::numeric)),
    CONSTRAINT chk_court_rate_nighttime CHECK ((rate_nighttime >= (0)::numeric)),
    CONSTRAINT chk_court_type CHECK (((court_type)::text = ANY ((ARRAY['Pickleball'::character varying, 'Basketball'::character varying])::text[])))
);


--
-- Name: customer_id_seq; Type: SEQUENCE; Schema: resort; Owner: -
--

CREATE SEQUENCE resort.customer_id_seq
    START WITH 102
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: customer; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.customer (
    customer_id character varying(10) DEFAULT ('CUST-'::text || lpad((nextval('resort.customer_id_seq'::regclass))::text, 3, '0'::text)) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    email character varying(120),
    phone character varying(20) NOT NULL,
    address character varying(255),
    customer_type character varying(20) DEFAULT 'Registered'::character varying NOT NULL,
    middle_name character varying(50),
    CONSTRAINT chk_customer_profile_completeness CHECK ((((customer_type)::text = 'Walk-In'::text) OR ((email IS NOT NULL) AND (phone IS NOT NULL) AND (address IS NOT NULL)))),
    CONSTRAINT chk_customer_type CHECK (((customer_type)::text = ANY ((ARRAY['Registered'::character varying, 'Walk-In'::character varying])::text[])))
);


--
-- Name: payment; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.payment (
    payment_id character varying(15) NOT NULL,
    booking_id character varying(15),
    order_id character varying(15),
    amount numeric(10,2) NOT NULL,
    method character varying(30) NOT NULL,
    payment_type character varying(20) NOT NULL,
    paid_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    reference_no character varying(50),
    CONSTRAINT chk_payment_amount CHECK ((amount > (0)::numeric)),
    CONSTRAINT chk_payment_exclusive_arc CHECK ((((booking_id IS NOT NULL) AND (order_id IS NULL)) OR ((booking_id IS NULL) AND (order_id IS NOT NULL)))),
    CONSTRAINT chk_payment_method CHECK (((method)::text = ANY ((ARRAY['Cash'::character varying, 'Maya'::character varying])::text[]))),
    CONSTRAINT chk_payment_type CHECK (((payment_type)::text = ANY ((ARRAY['Payment'::character varying, 'Refund'::character varying])::text[])))
);


--
-- Name: room; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.room (
    room_id character varying(10) NOT NULL,
    room_number character varying(10) NOT NULL,
    rate_per_day numeric(10,2) NOT NULL,
    max_pax integer NOT NULL,
    status character varying(20) NOT NULL,
    room_type character varying(20) DEFAULT 'Standard'::character varying NOT NULL,
    CONSTRAINT chk_room_max_pax CHECK ((max_pax > 0)),
    CONSTRAINT chk_room_rate_per_day CHECK ((rate_per_day >= (0)::numeric)),
    CONSTRAINT chk_room_status CHECK (((status)::text = ANY ((ARRAY['Ready'::character varying, 'Occupied'::character varying, 'Cleaning'::character varying, 'Maintenance'::character varying])::text[]))),
    CONSTRAINT chk_room_type CHECK (((room_type)::text = ANY ((ARRAY['Standard'::character varying, 'Deluxe'::character varying])::text[])))
);


--
-- Name: staff; Type: TABLE; Schema: resort; Owner: -
--

CREATE TABLE resort.staff (
    staff_id character varying(10) NOT NULL,
    first_name character varying(50) NOT NULL,
    last_name character varying(50) NOT NULL,
    role character varying(50) NOT NULL,
    middle_name character varying(50),
    CONSTRAINT chk_staff_role CHECK (((role)::text = ANY ((ARRAY['Front Desk'::character varying, 'Cashier'::character varying, 'Manager'::character varying, 'Admin'::character varying])::text[])))
);


--
-- Name: pos_item pk_pos_item; Type: CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_item
    ADD CONSTRAINT pk_pos_item PRIMARY KEY (item_id);


--
-- Name: pos_order pk_pos_order; Type: CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order
    ADD CONSTRAINT pk_pos_order PRIMARY KEY (order_id);


--
-- Name: pos_order_item pk_pos_order_item; Type: CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order_item
    ADD CONSTRAINT pk_pos_order_item PRIMARY KEY (order_id, item_id);


--
-- Name: booking_room booking_room_pkey; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking_room
    ADD CONSTRAINT booking_room_pkey PRIMARY KEY (booking_id, room_id);


--
-- Name: booking ex_court_no_overlap; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT ex_court_no_overlap EXCLUDE USING gist (court_id WITH =, tsrange((court_date + start_time), (court_date + end_time)) WITH &&) WHERE (((court_id IS NOT NULL) AND ((status)::text <> 'Cancelled'::text) AND (start_time IS NOT NULL) AND (end_time IS NOT NULL)));


--
-- Name: booking ex_exclusive_no_overlap; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT ex_exclusive_no_overlap EXCLUDE USING gist (daterange(check_in_date, check_out_date, '[)'::text) WITH &&) WHERE ((exclusive AND ((status)::text <> 'Cancelled'::text)));


--
-- Name: booking_room ex_room_no_overlap; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking_room
    ADD CONSTRAINT ex_room_no_overlap EXCLUDE USING gist (room_id WITH =, stay WITH &&) WHERE (is_active);


--
-- Name: payment payment_reference_no_key; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.payment
    ADD CONSTRAINT payment_reference_no_key UNIQUE (reference_no);


--
-- Name: booking pk_booking; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT pk_booking PRIMARY KEY (booking_id);


--
-- Name: court pk_court; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.court
    ADD CONSTRAINT pk_court PRIMARY KEY (court_id);


--
-- Name: customer pk_customer; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.customer
    ADD CONSTRAINT pk_customer PRIMARY KEY (customer_id);


--
-- Name: payment pk_payment; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.payment
    ADD CONSTRAINT pk_payment PRIMARY KEY (payment_id);


--
-- Name: room pk_room; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.room
    ADD CONSTRAINT pk_room PRIMARY KEY (room_id);


--
-- Name: staff pk_staff; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.staff
    ADD CONSTRAINT pk_staff PRIMARY KEY (staff_id);


--
-- Name: customer uq_customer_email; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.customer
    ADD CONSTRAINT uq_customer_email UNIQUE (email);


--
-- Name: room uq_room_number; Type: CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.room
    ADD CONSTRAINT uq_room_number UNIQUE (room_number);


--
-- Name: booking ctrg_booking_has_rooms; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE CONSTRAINT TRIGGER ctrg_booking_has_rooms AFTER INSERT OR UPDATE ON resort.booking DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION resort.fn_require_rooms();


--
-- Name: booking_room ctrg_booking_room_keeps_one; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE CONSTRAINT TRIGGER ctrg_booking_room_keeps_one AFTER DELETE ON resort.booking_room DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION resort.fn_require_rooms_after_delete();


--
-- Name: booking trg_booking_propagate_to_rooms; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE TRIGGER trg_booking_propagate_to_rooms AFTER UPDATE OF status, check_in_date, check_out_date ON resort.booking FOR EACH ROW EXECUTE FUNCTION resort.fn_propagate_booking_to_rooms();


--
-- Name: booking_room trg_enforce_br1_room_caps; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE TRIGGER trg_enforce_br1_room_caps BEFORE INSERT OR UPDATE ON resort.booking_room FOR EACH ROW EXECUTE FUNCTION resort.fn_enforce_br1_room_caps();


--
-- Name: booking trg_exclusive_booking_collision; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE TRIGGER trg_exclusive_booking_collision BEFORE INSERT OR UPDATE ON resort.booking FOR EACH ROW EXECUTE FUNCTION resort.fn_check_exclusive_collision();


--
-- Name: booking_room trg_sync_booking_room_stay; Type: TRIGGER; Schema: resort; Owner: -
--

CREATE TRIGGER trg_sync_booking_room_stay BEFORE INSERT OR UPDATE ON resort.booking_room FOR EACH ROW EXECUTE FUNCTION resort.fn_sync_booking_room_stay();


--
-- Name: pos_order fk_pos_order_booking; Type: FK CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order
    ADD CONSTRAINT fk_pos_order_booking FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pos_order fk_pos_order_customer; Type: FK CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order
    ADD CONSTRAINT fk_pos_order_customer FOREIGN KEY (customer_id) REFERENCES resort.customer(customer_id) ON UPDATE CASCADE ON DELETE SET NULL;


--
-- Name: pos_order_item fk_pos_order_item_item; Type: FK CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order_item
    ADD CONSTRAINT fk_pos_order_item_item FOREIGN KEY (item_id) REFERENCES pos.pos_item(item_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pos_order_item fk_pos_order_item_order; Type: FK CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order_item
    ADD CONSTRAINT fk_pos_order_item_order FOREIGN KEY (order_id) REFERENCES pos.pos_order(order_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: pos_order fk_pos_order_staff; Type: FK CONSTRAINT; Schema: pos; Owner: -
--

ALTER TABLE ONLY pos.pos_order
    ADD CONSTRAINT fk_pos_order_staff FOREIGN KEY (staff_id) REFERENCES resort.staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: booking_room booking_room_booking_id_fkey; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking_room
    ADD CONSTRAINT booking_room_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: booking_room booking_room_room_id_fkey; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking_room
    ADD CONSTRAINT booking_room_room_id_fkey FOREIGN KEY (room_id) REFERENCES resort.room(room_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: booking fk_booking_court; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT fk_booking_court FOREIGN KEY (court_id) REFERENCES resort.court(court_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: booking fk_booking_customer; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT fk_booking_customer FOREIGN KEY (customer_id) REFERENCES resort.customer(customer_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: booking fk_booking_staff; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.booking
    ADD CONSTRAINT fk_booking_staff FOREIGN KEY (staff_id) REFERENCES resort.staff(staff_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: payment fk_payment_booking; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.payment
    ADD CONSTRAINT fk_payment_booking FOREIGN KEY (booking_id) REFERENCES resort.booking(booking_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: payment fk_payment_order; Type: FK CONSTRAINT; Schema: resort; Owner: -
--

ALTER TABLE ONLY resort.payment
    ADD CONSTRAINT fk_payment_order FOREIGN KEY (order_id) REFERENCES pos.pos_order(order_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: pos_item; Type: ROW SECURITY; Schema: pos; Owner: -
--

ALTER TABLE pos.pos_item ENABLE ROW LEVEL SECURITY;

--
-- Name: pos_order; Type: ROW SECURITY; Schema: pos; Owner: -
--

ALTER TABLE pos.pos_order ENABLE ROW LEVEL SECURITY;

--
-- Name: pos_order_item; Type: ROW SECURITY; Schema: pos; Owner: -
--

ALTER TABLE pos.pos_order_item ENABLE ROW LEVEL SECURITY;

--
-- Name: booking; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.booking ENABLE ROW LEVEL SECURITY;

--
-- Name: court; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.court ENABLE ROW LEVEL SECURITY;

--
-- Name: customer; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.customer ENABLE ROW LEVEL SECURITY;

--
-- Name: payment; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.payment ENABLE ROW LEVEL SECURITY;

--
-- Name: room; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.room ENABLE ROW LEVEL SECURITY;

--
-- Name: staff; Type: ROW SECURITY; Schema: resort; Owner: -
--

ALTER TABLE resort.staff ENABLE ROW LEVEL SECURITY;

--
-- PostgreSQL database dump complete
--

\unrestrict ZuH9SaCfQVZQGevR9n8NDWkAKWT2m6cD8DGlConQJN0J9TrDktT4ImIWR3Esuyj

