--
-- PostgreSQL database dump
--

\restrict 1b3HVyLxJXlunvto6JsE9pS49XmToQpyn4DEcoljgWWfed0dSlHjczxtOsgoYCO

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
-- Data for Name: pos_item; Type: TABLE DATA; Schema: pos; Owner: -
--

COPY pos.pos_item (item_id, name, category, price) FROM stdin;
ITM-V01	Iced Americano	Beverage	120.00
ITM-V02	Club Sandwich	Food	260.00
\.


--
-- Data for Name: pos_order; Type: TABLE DATA; Schema: pos; Owner: -
--

COPY pos.pos_order (order_id, customer_id, staff_id, order_date, total_amount, booking_id) FROM stdin;
\.


--
-- Data for Name: pos_order_item; Type: TABLE DATA; Schema: pos; Owner: -
--

COPY pos.pos_order_item (order_id, item_id, quantity, subtotal) FROM stdin;
\.


--
-- Data for Name: booking; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.booking (booking_id, customer_id, staff_id, court_id, booking_date, start_time, end_time, check_in_date, check_out_date, exclusive, paddle_count, status, created_at, hold_expires_at, total_amount, court_date) FROM stdin;
BK-8X902E35	CUST-007	STF-001	\N	2026-10-15	\N	\N	2026-10-15	2026-10-16	f	0	Completed	2026-10-15 02:30:39.632788+00	2026-10-15 02:45:39.632788+00	2500.00	\N
BK-8XKKTJ39	CUST-008	STF-001	\N	2026-10-15	\N	\N	2026-10-15	2026-10-16	f	0	Pending	2026-10-15 02:39:38.71951+00	2026-10-15 02:54:38.71951+00	2500.00	\N
BK-AKWNB296	CUST-010	STF-001	\N	2026-10-16	\N	\N	2026-10-16	2026-10-17	f	0	Confirmed	2026-10-16 06:20:40.142998+00	2026-10-16 06:35:40.142998+00	2500.00	\N
BK-498173	CUST-009	STF-001	\N	2026-10-17	\N	\N	2026-10-17	2026-10-18	f	0	Pending	2026-10-17 03:24:59.242771+00	2026-10-17 03:39:59.242771+00	0.00	\N
BK-BUWHU465	CUST-011	STF-001	\N	2026-10-17	\N	\N	2026-10-31	2026-11-07	f	0	Pending	2026-10-17 03:48:16.10326+00	2026-10-17 04:03:16.10326+00	17500.00	\N
BK-BUSX1S15	CUST-011	STF-001	\N	2026-10-17	\N	\N	2026-10-23	2026-11-13	f	0	Cancelled	2026-10-17 03:45:29.199708+00	2026-10-17 04:00:29.199708+00	52500.00	\N
BK-BVWMZD98	CUST-011	STF-001	\N	2026-10-17	\N	\N	2026-10-17	2026-10-18	f	0	Pending	2026-10-17 04:16:22.381584+00	2026-10-17 04:31:22.381584+00	2500.00	\N
BK-C945BM51	CUST-011	STF-001	\N	2026-10-17	\N	\N	2026-10-17	2026-10-18	f	0	Pending	2026-10-17 10:26:07.082232+00	2026-10-17 10:41:07.082232+00	3500.00	\N
BK-261017-101	CUST-011	STF-001	CRT-PB1	2026-10-17	12:00:00	13:00:00	\N	\N	f	1	Confirmed	2026-10-17 03:51:08.720712+00	2026-10-17 04:06:08.720712+00	500.00	2026-10-17
\.


--
-- Data for Name: booking_room; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.booking_room (booking_id, room_id, booked_rate, stay, is_active) FROM stdin;
BK-8X902E35	RM-102	2500.00	[2026-10-15,2026-10-16)	t
BK-8XKKTJ39	RM-101	2500.00	[2026-10-15,2026-10-16)	t
BK-AKWNB296	RM-101	2500.00	[2026-10-16,2026-10-17)	t
BK-498173	RM-101	2500.00	[2026-10-17,2026-10-18)	t
BK-BUWHU465	RM-102	2500.00	[2026-10-31,2026-11-07)	t
BK-BUSX1S15	RM-101	2500.00	[2026-10-23,2026-11-13)	f
BK-BVWMZD98	RM-102	2500.00	[2026-10-17,2026-10-18)	t
BK-C945BM51	RM-103	2500.00	[2026-10-17,2026-10-18)	t
\.


--
-- Data for Name: court; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.court (court_id, court_name, court_type, rate_daytime, rate_nighttime, paddle_rate) FROM stdin;
CRT-01	Court CRT-01	Pickleball	400.00	600.00	100.00
CRT-02	Court CRT-02	Pickleball	400.00	600.00	100.00
CRT-PB1	Court CRT-PB1	Pickleball	400.00	600.00	100.00
\.


--
-- Data for Name: customer; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.customer (customer_id, first_name, last_name, email, phone, address, customer_type, middle_name) FROM stdin;
CUST-008	Des	C	des@gmail.com	09691331023	asd ||CVAUTH:desi|08f62b384fba$73921ce5d93aea68728e2230a9202a3b|10/15/2008|Female	Registered	A
CUST-007	Vilma	Abarquez	vilma.abarquez70@gmail.com	09123456789	Cebu City	Registered	\N
CUST-010	Russell John	Astrologo	russelljohnastrologo@gmail.com	09123456789	Cebu City	Registered	\N
CUST-009	test12345	User	test12345@casavista.com	09123456789	Cebu City	Registered	\N
CUST-011	JOHN	CABILAO	cabilaojohnmichaell@gmail.com	09087568133	Mabolo Cebu City ||CVAUTH:mama mo|46fd9734004f$92a5a1cf38af370fc45771cef17cda3d|10/21/2007|Non-binary	Registered	LUCERO
CUST-999	Guest	Evaluator	guest.evaluator@casavista.test	09179998888	Cebu City ||CVAUTH:guest_evaluator|a1b2c3d4e5f6$437c6278df9911a4f13a9f7fc9a4e734|2000-01-01|Male	Registered	T.
\.


--
-- Data for Name: payment; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.payment (payment_id, booking_id, order_id, amount, method, payment_type, paid_at, reference_no) FROM stdin;
PAY-1458156	BK-8X902E35	\N	2500.00	Maya	Payment	2026-10-15 02:30:58.521704+00	\N
PAY-1658685	BK-AKWNB296	\N	2500.00	Maya	Payment	2026-10-16 06:20:58.891453+00	\N
PAY-8953423	BK-BUSX1S15	\N	52500.00	Maya	Payment	2026-10-17 03:49:14.488089+00	\N
PAY-594916	BK-261017-101	\N	2500.00	Maya	Payment	2026-10-17 04:16:35.984094+00	\N
\.


--
-- Data for Name: room; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.room (room_id, room_number, rate_per_day, max_pax, status, room_type) FROM stdin;
RM-102	102	2500.00	4	Ready	Standard
RM-101	101	2500.00	4	Ready	Standard
RM-103	103	2500.00	4	Ready	Standard
\.


--
-- Data for Name: staff; Type: TABLE DATA; Schema: resort; Owner: -
--

COPY resort.staff (staff_id, first_name, last_name, role, middle_name) FROM stdin;
STF-001	System	Staff	Admin	\N
STF-002	Cashier	Staff	Cashier	\N
STF-005	Russell	John Astrologo	Admin	\N
\.


--
-- Name: customer_id_seq; Type: SEQUENCE SET; Schema: resort; Owner: -
--

SELECT pg_catalog.setval('resort.customer_id_seq', 11, true);


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

\unrestrict 1b3HVyLxJXlunvto6JsE9pS49XmToQpyn4DEcoljgWWfed0dSlHjczxtOsgoYCO

