# pezesha
Core banking Loan System

To start this Application you need database created and tables.Therefore kindly run the script below if the scripts in db.sql does not run.


SQL SCRIPTS

--
-- PostgreSQL database dump
--

-- Dumped from database version 16.4
-- Dumped by pg_dump version 16.4

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: repayments; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.repayments (
id bigint NOT NULL,
repayment_id character varying(255) NOT NULL,
loan_id character varying(255) NOT NULL,
amount numeric(20,4) NOT NULL,
payment_date timestamp with time zone NOT NULL,
principal_paid numeric(20,4) DEFAULT 0.0000,
interest_paid numeric(20,4) DEFAULT 0.0000,
allocation_strategy character varying(100),
payment_account_id character varying(100),
transaction_id character varying(255),
created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
updated_at timestamp without time zone,
is_deleted boolean DEFAULT false
);


ALTER TABLE public.repayments OWNER TO cblms_user;

--
-- Name: repayments_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.repayments ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.repayments_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: tb_accounts; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_accounts (
id bigint DEFAULT (floor(((random() * ('9000000000'::bigint)::double precision) + (1000000000)::double precision)))::bigint NOT NULL,
code character varying(100) NOT NULL,
name character varying(255) NOT NULL,
type character varying(50) NOT NULL,
currency character varying(3) NOT NULL,
parent_account_id bigint,
current_balance numeric(20,4) DEFAULT 0.0000,
is_active boolean DEFAULT false,
has_transaction boolean DEFAULT false,
created_at timestamp without time zone,
created_by character varying(50),
updated_at timestamp without time zone,
is_deleted boolean DEFAULT false
);


ALTER TABLE public.tb_accounts OWNER TO cblms_user;

--
-- Name: tb_borrowers; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_borrowers (
id bigint NOT NULL,
borrower_id character varying(255) NOT NULL,
name character varying(255) NOT NULL,
created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
updated_at timestamp with time zone,
is_deleted boolean DEFAULT false
);


ALTER TABLE public.tb_borrowers OWNER TO cblms_user;

--
-- Name: tb_borrowers_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.tb_borrowers ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.tb_borrowers_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: tb_idempotency; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_idempotency (
id bigint NOT NULL,
key character varying(255) NOT NULL,
transaction_id bigint NOT NULL,
created_at timestamp with time zone NOT NULL
);


ALTER TABLE public.tb_idempotency OWNER TO cblms_user;

--
-- Name: tb_idempotency_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.tb_idempotency ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.tb_idempotency_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: tb_journal_entries; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_journal_entries (
id bigint NOT NULL,
transaction_id bigint NOT NULL,
account_id bigint NOT NULL,
account_code character varying(50) NOT NULL,
account_name character varying(100) NOT NULL,
account_type character varying(50) NOT NULL,
debit numeric(15,2) DEFAULT 0.00 NOT NULL,
credit numeric(15,2) DEFAULT 0.00 NOT NULL,
transaction_date timestamp with time zone NOT NULL
);


ALTER TABLE public.tb_journal_entries OWNER TO cblms_user;

--
-- Name: tb_journal_entries_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.tb_journal_entries ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.tb_journal_entries_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: tb_loans; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_loans (
id bigint NOT NULL,
loan_id character varying(255) NOT NULL,
borrower_id character varying(255) NOT NULL,
principal_amount numeric(20,4) NOT NULL,
interest_rate numeric(20,4) NOT NULL,
origination_fee numeric(20,4) DEFAULT 0.0000,
currency character varying(3) NOT NULL,
disbursement_date timestamp with time zone,
maturity_date timestamp with time zone,
status character varying(50) NOT NULL,
outstanding_principal numeric(20,4) DEFAULT 0.0000,
outstanding_interest numeric(20,4) DEFAULT 0.0000,
total_paid numeric(20,4) DEFAULT 0.0000,
last_payment_date timestamp with time zone,
closure_date timestamp with time zone,
created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
updated_at timestamp with time zone,
created_by character varying(255),
updated_by character varying(255),
is_deleted boolean DEFAULT false
);


ALTER TABLE public.tb_loans OWNER TO cblms_user;

--
-- Name: tb_loans_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.tb_loans ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.tb_loans_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: tb_transactions; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.tb_transactions (
id bigint NOT NULL,
transaction_id character varying(255) NOT NULL,
idempotency_key character varying(255) NOT NULL,
status character varying(50) NOT NULL,
transaction_date timestamp with time zone NOT NULL,
description character varying(500) NOT NULL,
currency character varying(3) NOT NULL,
total_debit numeric(15,2) DEFAULT 0.00 NOT NULL,
total_credit numeric(15,2) DEFAULT 0.00 NOT NULL,
posted_at timestamp with time zone NOT NULL,
reversed_by character varying(255),
reversal_transaction_id bigint,
reversal_date timestamp with time zone,
reason character varying(500),
created_by character varying(255),
created_at timestamp with time zone NOT NULL,
updated_at timestamp with time zone
);


ALTER TABLE public.tb_transactions OWNER TO cblms_user;

--
-- Name: tb_transactions_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.tb_transactions ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.tb_transactions_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Name: write_offs; Type: TABLE; Schema: public; Owner: cblms_user
--

CREATE TABLE public.write_offs (
id bigint NOT NULL,
write_off_id character varying(255) NOT NULL,
loan_id character varying(255) NOT NULL,
write_off_date timestamp with time zone NOT NULL,
write_off_type character varying(100) NOT NULL,
principal_written_off numeric(20,4) DEFAULT 0.0000,
interest_written_off numeric(20,4) DEFAULT 0.0000,
total_written_off numeric(20,4) DEFAULT 0.0000,
reason character varying(500),
transaction_id character varying(255),
created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
updated_at timestamp without time zone,
is_deleted boolean DEFAULT false
);


ALTER TABLE public.write_offs OWNER TO cblms_user;

--
-- Name: write_offs_id_seq; Type: SEQUENCE; Schema: public; Owner: cblms_user
--

ALTER TABLE public.write_offs ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
SEQUENCE NAME public.write_offs_id_seq
START WITH 1
INCREMENT BY 1
NO MINVALUE
NO MAXVALUE
CACHE 1
);


--
-- Data for Name: repayments; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.repayments (id, repayment_id, loan_id, amount, payment_date, principal_paid, interest_paid, allocation_strategy, payment_account_id, transaction_id, created_at, updated_at, is_deleted) FROM stdin;
1	repay_2e3c8bc0	loan_8e89c4a1	1000.0000	2025-02-10 13:00:00+03	997.2600	2.7400	INTEREST_FIRST	10100	\N	2026-02-06 20:09:12.06565	\N	f
2	repay_2eccd8d4	loan_8e89c4a1	1000.0000	2025-02-10 13:00:00+03	997.2600	2.7400	INTEREST_FIRST	10100	\N	2026-02-06 20:10:26.925494	\N	f
\.


--
-- Data for Name: tb_accounts; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_accounts (id, code, name, type, currency, parent_account_id, current_balance, is_active, has_transaction, created_at, created_by, updated_at, is_deleted) FROM stdin;
9066888418	1015	Cash on Hand	ASSET	KES	\N	0.0000	f	f	2026-02-05 16:24:01.371068	admin.user	\N	f
6485613494	1012	Cash MPESA	ASSET	KES	9066888418	500.0000	t	t	2026-02-05 22:43:36.557626	admin.user	2026-02-06 13:01:29.974924	t
9038292509	40001	Fee Income	INCOME	KES	\N	0.0000	t	f	\N	\N	\N	f
1337491638	40002	Interest Income	INCOME	KES	\N	0.0000	t	f	\N	\N	\N	f
2153255326	50001	Bad Debt Expense	EXPENSE	KES	\N	0.0000	t	f	\N	\N	\N	f
3145510726	10189	Cash on MPESA	ASSET	KES	9066888418	15000.0000	t	t	2026-02-06 12:05:25.939249	admin.user	2026-02-06 19:27:48.028198	f
2247676669	10001	Loans Receivable	ASSET	KES	\N	7000.0000	t	t	\N	\N	2026-02-06 20:06:18.361001	f
6643038539	10100	Cash - M-Pesa	ASSET	KES	\N	88000.0000	t	t	\N	\N	2026-02-06 20:06:18.361001	f
\.


--
-- Data for Name: tb_borrowers; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_borrowers (id, borrower_id, name, created_at, updated_at, is_deleted) FROM stdin;
\.


--
-- Data for Name: tb_idempotency; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_idempotency (id, key, transaction_id, created_at) FROM stdin;
1	disbursement-loan-12345	2	2026-02-06 12:14:56.894634+03
2	disbursement-loan-123745	3	2026-02-06 12:16:04.64092+03
3	disbursement-loan-1293745	4	2026-02-06 12:16:17.976849+03
4	reversal-txn-67890	6	2026-02-06 12:34:31.57815+03
5	disbursement-loan-12993745	7	2026-02-06 12:53:20.670648+03
6	reversal-txn-687890	8	2026-02-06 12:56:08.172459+03
7	disbursement-loan-1299385	9	2026-02-06 13:01:30.052033+03
8	reversal-txn-6878750	10	2026-02-06 13:01:49.142594+03
9	idem-9002_disb	11	2026-02-06 19:27:48.086245+03
10	idem-90092_disb	12	2026-02-06 19:53:14.914945+03
11	idem-90092	9	2026-02-06 20:02:32.514065+03
12	idem-900920_disb	13	2026-02-06 20:06:18.376545+03
13	idem-900920	11	2026-02-06 20:08:27.984911+03
14	idem_8399022	2	2026-02-06 20:10:26.950933+03
15	writeoff-00292	2	2026-02-06 20:12:40.823258+03
\.


--
-- Data for Name: tb_journal_entries; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_journal_entries (id, transaction_id, account_id, account_code, account_name, account_type, debit, credit, transaction_date) FROM stdin;
1	2	6485613494	1012	Cash MPESA	ASSET	10000.00	0.00	2025-02-03 17:30:00+03
2	2	3145510726	10189	Cash on MPESA	ASSET	0.00	10000.00	2025-02-03 17:30:00+03
3	3	3145510726	10189	Cash on MPESA	ASSET	10000.00	0.00	2025-02-03 17:30:00+03
4	3	6485613494	1012	Cash MPESA	ASSET	0.00	10000.00	2025-02-03 17:30:00+03
5	4	6485613494	1012	Cash MPESA	ASSET	10000.00	0.00	2025-02-03 17:30:00+03
6	4	3145510726	10189	Cash on MPESA	ASSET	0.00	10000.00	2025-02-03 17:30:00+03
7	6	6485613494	1012	Cash MPESA	ASSET	0.00	10000.00	2025-02-04 13:00:00+03
8	6	3145510726	10189	Cash on MPESA	ASSET	10000.00	0.00	2025-02-04 13:00:00+03
9	7	6485613494	1012	Cash MPESA	ASSET	10000.00	0.00	2025-02-03 17:30:00+03
10	7	3145510726	10189	Cash on MPESA	ASSET	0.00	10000.00	2025-02-03 17:30:00+03
11	8	6485613494	1012	Cash MPESA	ASSET	0.00	10000.00	2025-02-04 13:00:00+03
12	8	3145510726	10189	Cash on MPESA	ASSET	10000.00	0.00	2025-02-04 13:00:00+03
13	9	3145510726	10189	Cash on MPESA	ASSET	10000.00	0.00	2025-02-03 17:30:00+03
14	9	6485613494	1012	Cash MPESA	ASSET	0.00	10000.00	2025-02-03 17:30:00+03
15	10	3145510726	10189	Cash on MPESA	ASSET	0.00	10000.00	2025-02-04 13:00:00+03
16	10	6485613494	1012	Cash MPESA	ASSET	10000.00	0.00	2025-02-04 13:00:00+03
17	11	3145510726	10189	Cash on MPESA	ASSET	5000.00	0.00	2025-02-06 13:00:00+03
18	11	6643038539	10100	Cash - M-Pesa	ASSET	0.00	5000.00	2025-02-06 13:00:00+03
19	12	2247676669	10001	Loans Receivable	ASSET	5000.00	0.00	2025-02-06 13:00:00+03
20	12	6643038539	10100	Cash - M-Pesa	ASSET	0.00	5000.00	2025-02-06 13:00:00+03
21	13	2247676669	10001	Loans Receivable	ASSET	2000.00	0.00	2025-02-06 13:00:00+03
22	13	6643038539	10100	Cash - M-Pesa	ASSET	0.00	2000.00	2025-02-06 13:00:00+03
\.


--
-- Data for Name: tb_loans; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_loans (id, loan_id, borrower_id, principal_amount, interest_rate, origination_fee, currency, disbursement_date, maturity_date, status, outstanding_principal, outstanding_interest, total_paid, last_payment_date, closure_date, created_at, updated_at, created_by, updated_by, is_deleted) FROM stdin;
4	loan_e02748c3	borrower_92002	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 19:22:14.195821+03	\N	\N	\N	f
5	loan_cd082a07	borrower_92002	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 19:26:34.921668+03	\N	\N	\N	f
6	loan_51f247a3	borrower_92002	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 19:27:47.924848+03	\N	\N	\N	f
7	loan_863c7b50	borrower_92002	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 19:41:04.420434+03	\N	\N	\N	f
8	loan_aa4fc360	borrower_9207	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 19:53:14.8541+03	\N	\N	\N	f
9	loan_f4889345	borrower_9207	5000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	5000.0000	0.0000	0.0000	\N	\N	2026-02-06 20:02:32.459055+03	\N	\N	\N	f
10	loan_21844c59	borrower_92079	2000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	ACTIVE	2000.0000	0.0000	0.0000	\N	\N	2026-02-06 20:06:18.305875+03	\N	\N	\N	f
11	loan_8e89c4a1	borrower_92079	2000.0000	12.5000	250.0000	KES	2025-02-06 13:00:00+03	2025-08-06 13:00:00+03	WRITTEN_OFF	0.0000	0.0000	1000.0000	2025-02-10 13:00:00+03	\N	2026-02-06 20:08:27.924947+03	\N	\N	\N	f
\.


--
-- Data for Name: tb_transactions; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.tb_transactions (id, transaction_id, idempotency_key, status, transaction_date, description, currency, total_debit, total_credit, posted_at, reversed_by, reversal_transaction_id, reversal_date, reason, created_by, created_at, updated_at) FROM stdin;
2	txn_0ba8ae5f	disbursement-loan-12345	POSTED	2025-02-03 17:30:00+03	Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:14:56.846183+03	\N	\N	\N	\N	\N	2026-02-06 12:14:56.84718+03	\N
3	txn_5d9a1728	disbursement-loan-123745	POSTED	2025-02-03 17:30:00+03	Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:16:04.588289+03	\N	\N	\N	\N	\N	2026-02-06 12:16:04.588289+03	\N
4	txn_02bdc113	disbursement-loan-1293745	POSTED	2025-02-03 17:30:00+03	Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:16:17.919548+03	\N	\N	\N	\N	\N	2026-02-06 12:16:17.920546+03	\N
6	txn_ea10e943	reversal-txn-67890	POSTED	2025-02-04 13:00:00+03	Reversal of Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:34:31.538095+03	system	\N	\N	Loan disbursement error - borrower not eligible	\N	2026-02-06 12:34:31.539145+03	\N
7	txn_ffac2ff2	disbursement-loan-12993745	POSTED	2025-02-03 17:30:00+03	Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:53:20.620168+03	\N	\N	\N	\N	\N	2026-02-06 12:53:20.620168+03	\N
8	txn_bfa0defe	reversal-txn-687890	POSTED	2025-02-04 13:00:00+03	Reversal of Loan disbursement	KES	10000.00	10000.00	2026-02-06 12:56:08.125474+03	system	\N	\N	Loan disbursement error - borrower not eligible	\N	2026-02-06 12:56:08.125474+03	\N
10	txn_faadfa06	reversal-txn-6878750	POSTED	2025-02-04 13:00:00+03	Reversal of Loan disbursement	KES	10000.00	10000.00	2026-02-06 13:01:49.060432+03	system	\N	\N	Loan disbursement error - borrower not eligible	\N	2026-02-06 13:01:49.060432+03	\N
9	txn_fa8a02f5	disbursement-loan-1299385	REVERSED	2025-02-03 17:30:00+03	Loan disbursement	KES	10000.00	10000.00	2026-02-06 13:01:29.766799+03	system	10	2025-02-04 13:00:00+03	\N	\N	2026-02-06 13:01:29.767797+03	2026-02-06 13:01:49.179446+03
11	txn_720a789a	idem-9002_disb	POSTED	2025-02-06 13:00:00+03	Loan disbursement for loan_51f247a3	KES	5000.00	5000.00	2026-02-06 19:27:47.962762+03	\N	\N	\N	\N	\N	2026-02-06 19:27:47.96376+03	\N
12	txn_94d7c4c3	idem-90092_disb	POSTED	2025-02-06 13:00:00+03	Loan disbursement for loan_aa4fc360	KES	5000.00	5000.00	2026-02-06 19:53:14.883448+03	\N	\N	\N	\N	\N	2026-02-06 19:53:14.883448+03	\N
13	txn_913bfee6	idem-900920_disb	POSTED	2025-02-06 13:00:00+03	Loan disbursement for loan_21844c59	KES	2000.00	2000.00	2026-02-06 20:06:18.337968+03	\N	\N	\N	\N	\N	2026-02-06 20:06:18.337968+03	\N
\.


--
-- Data for Name: write_offs; Type: TABLE DATA; Schema: public; Owner: cblms_user
--

COPY public.write_offs (id, write_off_id, loan_id, write_off_date, write_off_type, principal_written_off, interest_written_off, total_written_off, reason, transaction_id, created_at, updated_at, is_deleted) FROM stdin;
1	writeoff_0d8d7140	loan_8e89c4a1	2025-03-01 13:00:00+03	FULL	1002.7400	0.0000	1002.7400	Borrower deceased	\N	2026-02-06 20:11:59.461577	\N	f
2	writeoff_f5a2545c	loan_8e89c4a1	2025-03-01 13:00:00+03	FULL	1002.7400	0.0000	1002.7400	Borrower deceased	\N	2026-02-06 20:12:40.795599	\N	f
\.


--
-- Name: repayments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.repayments_id_seq', 2, true);


--
-- Name: tb_borrowers_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.tb_borrowers_id_seq', 1, false);


--
-- Name: tb_idempotency_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.tb_idempotency_id_seq', 15, true);


--
-- Name: tb_journal_entries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.tb_journal_entries_id_seq', 22, true);


--
-- Name: tb_loans_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.tb_loans_id_seq', 11, true);


--
-- Name: tb_transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.tb_transactions_id_seq', 13, true);


--
-- Name: write_offs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: cblms_user
--

SELECT pg_catalog.setval('public.write_offs_id_seq', 2, true);


--
-- Name: repayments repayments_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.repayments
ADD CONSTRAINT repayments_pkey PRIMARY KEY (id);


--
-- Name: repayments repayments_repayment_id_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.repayments
ADD CONSTRAINT repayments_repayment_id_key UNIQUE (repayment_id);


--
-- Name: tb_accounts tb_accounts_code_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_accounts
ADD CONSTRAINT tb_accounts_code_key UNIQUE (code);


--
-- Name: tb_accounts tb_accounts_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_accounts
ADD CONSTRAINT tb_accounts_pkey PRIMARY KEY (id);


--
-- Name: tb_borrowers tb_borrowers_borrower_id_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_borrowers
ADD CONSTRAINT tb_borrowers_borrower_id_key UNIQUE (borrower_id);


--
-- Name: tb_borrowers tb_borrowers_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_borrowers
ADD CONSTRAINT tb_borrowers_pkey PRIMARY KEY (id);


--
-- Name: tb_idempotency tb_idempotency_key_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_idempotency
ADD CONSTRAINT tb_idempotency_key_key UNIQUE (key);


--
-- Name: tb_idempotency tb_idempotency_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_idempotency
ADD CONSTRAINT tb_idempotency_pkey PRIMARY KEY (id);


--
-- Name: tb_journal_entries tb_journal_entries_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_journal_entries
ADD CONSTRAINT tb_journal_entries_pkey PRIMARY KEY (id);


--
-- Name: tb_loans tb_loans_loan_id_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_loans
ADD CONSTRAINT tb_loans_loan_id_key UNIQUE (loan_id);


--
-- Name: tb_loans tb_loans_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_loans
ADD CONSTRAINT tb_loans_pkey PRIMARY KEY (id);


--
-- Name: tb_transactions tb_transactions_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_transactions
ADD CONSTRAINT tb_transactions_pkey PRIMARY KEY (id);


--
-- Name: tb_transactions tb_transactions_transaction_id_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_transactions
ADD CONSTRAINT tb_transactions_transaction_id_key UNIQUE (transaction_id);


--
-- Name: write_offs write_offs_pkey; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.write_offs
ADD CONSTRAINT write_offs_pkey PRIMARY KEY (id);


--
-- Name: write_offs write_offs_write_off_id_key; Type: CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.write_offs
ADD CONSTRAINT write_offs_write_off_id_key UNIQUE (write_off_id);


--
-- Name: repayments repayments_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.repayments
ADD CONSTRAINT repayments_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.tb_loans(loan_id);


--
-- Name: repayments repayments_payment_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.repayments
ADD CONSTRAINT repayments_payment_account_id_fkey FOREIGN KEY (payment_account_id) REFERENCES public.tb_accounts(code);


--
-- Name: repayments repayments_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.repayments
ADD CONSTRAINT repayments_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.tb_transactions(transaction_id);


--
-- Name: tb_idempotency tb_idempotency_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_idempotency
ADD CONSTRAINT tb_idempotency_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.tb_transactions(id);


--
-- Name: tb_journal_entries tb_journal_entries_account_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_journal_entries
ADD CONSTRAINT tb_journal_entries_account_id_fkey FOREIGN KEY (account_id) REFERENCES public.tb_accounts(id);


--
-- Name: tb_journal_entries tb_journal_entries_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_journal_entries
ADD CONSTRAINT tb_journal_entries_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.tb_transactions(id);


--
-- Name: tb_transactions tb_transactions_reversal_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.tb_transactions
ADD CONSTRAINT tb_transactions_reversal_transaction_id_fkey FOREIGN KEY (reversal_transaction_id) REFERENCES public.tb_transactions(id);


--
-- Name: write_offs write_offs_loan_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.write_offs
ADD CONSTRAINT write_offs_loan_id_fkey FOREIGN KEY (loan_id) REFERENCES public.tb_loans(loan_id);


--
-- Name: write_offs write_offs_transaction_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: cblms_user
--

ALTER TABLE ONLY public.write_offs
ADD CONSTRAINT write_offs_transaction_id_fkey FOREIGN KEY (transaction_id) REFERENCES public.tb_transactions(transaction_id);


--
-- PostgreSQL database dump complete
--





Once the application is successfully running use the share Postman collection to do the test.



