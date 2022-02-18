--
-- PostgreSQL database dump
--

-- Dumped from database version 13.1
-- Dumped by pg_dump version 13.1

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
-- Name: config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.config (
    name text NOT NULL,
    value text
);


ALTER TABLE public.config OWNER TO postgres;

--
-- Name: items; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.items (
    id bigint NOT NULL,
    profile_id bigint NOT NULL,
    kind smallint NOT NULL,
    category bytea NOT NULL,
    name bytea NOT NULL,
    value bytea NOT NULL,
    expiry timestamp without time zone
);


ALTER TABLE public.items OWNER TO postgres;

--
-- Name: items_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.items_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.items_id_seq OWNER TO postgres;

--
-- Name: items_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.items_id_seq OWNED BY public.items.id;


--
-- Name: items_tags; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.items_tags (
    id bigint NOT NULL,
    item_id bigint NOT NULL,
    name bytea NOT NULL,
    value bytea NOT NULL,
    plaintext smallint NOT NULL
);


ALTER TABLE public.items_tags OWNER TO postgres;

--
-- Name: items_tags_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.items_tags_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.items_tags_id_seq OWNER TO postgres;

--
-- Name: items_tags_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.items_tags_id_seq OWNED BY public.items_tags.id;


--
-- Name: profiles; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.profiles (
    id bigint NOT NULL,
    name text NOT NULL,
    reference text,
    profile_key bytea
);


ALTER TABLE public.profiles OWNER TO postgres;

--
-- Name: profiles_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.profiles_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.profiles_id_seq OWNER TO postgres;

--
-- Name: profiles_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.profiles_id_seq OWNED BY public.profiles.id;


--
-- Name: items id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items ALTER COLUMN id SET DEFAULT nextval('public.items_id_seq'::regclass);


--
-- Name: items_tags id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items_tags ALTER COLUMN id SET DEFAULT nextval('public.items_tags_id_seq'::regclass);


--
-- Name: profiles id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profiles ALTER COLUMN id SET DEFAULT nextval('public.profiles_id_seq'::regclass);


--
-- Data for Name: config; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.config (name, value) FROM stdin;
default_profile	d3de636d-3e2f-4d73-89ae-c7cb902c62f6
key	kdf:argon2i:13:mod?salt=940895173841334ae74a799caec2f8e8
version	1
\.


--
-- Data for Name: items; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.items (id, profile_id, kind, category, name, value, expiry) FROM stdin;
2	1	1	\\x610b7ba4abe017d10df12bcf7cbe31e9f9b59d5dd488ed204bc489ba3b0b066d3bcc87263b	\\x23258a39c545115b964ab6d71999fa5eb11997aebd12dc4d7dee15637303d9a0355de1bf3066833cecc7f4cb636679eafd17fb0639e19273e914f19fcda10a812528d0a0eabe35ef	\\x17fddb4eb6046647b22d5259148300fb8aceffe42d7324feb9bc126c2c246b30cfe4f50ad5db438aa3dede7f1bcf3350c407c2881005b4d5189ec915d4a3579e0b0a3cd24da7989ed2ff8c7ed98f0e9b75c3e14a8da3e45d8891cd8b98e298c1bd80fb0737cd719e6e1542cb192c9d3c5987188a9c0e6d992c5d40ea539865e6f9019c8d9a13ae7cd21449b08c2f316f49d8bb3bcd57c80bda5fd3076e685ca9a753bdccd2f233aa7fd237754aaaef679fbe2f8516cb6223515d33	\N
4	1	2	\\xbb85dfc52874af6fb5f7bf8bcd44e02ab6b0a573a87bd13d0610201c25bdbf7b65f9	\\x34e5e8ab06dbc3c978048da80e3590bdd5a724d02d4feffb8ee2a275c207cb1bd344b595fa408416f009a05d66a4	\\x8a4062d56ef19390ca6ad935acc6528927962670ddd9b315936effee7c0dbcc2c8f1f27e792b0761960fe5ee655f7bcce4990643a8f6af6f5541e7c29b	\N
5	1	2	\\x8516cf38f9a58400f0b2a569bb12e6623aded0c8b37441e2b591684748830f	\\x31469c5a4402d587a9dd9f1f5d05fdbe88caff4872cb9da5a6428948a8a3a73c0c83f0957748bfad9e2328d1dac3caa34492	\\x4c086f06ab3577c905a6eea0bec9cb46cf88e5e8e6a2443405c1fe1a764f812972bd424210dbcf1b19bd380920449cf0ff41e19f4fbc26ad55b09dc836cabafe0c89a208a24d2b93f95eff0d66cf3a20977f2e3c8619ecefed87c9b90977904a25836b7fa13b18ce7412266a8ea248a13ccf323842c49172e6729457fe7afe8d9c92b6d6d84accb164257ae1d48b0da3117663a4cddcd66da2d33e389ddfe7ef470cca1a5b9d095cacb57b1d5ea41939c7acbad632971e5016c94964ff9b17c467f9f9f8c2831b994d37ef0059f65294fafd01274fc0d358b029964db6d004bbd1d17958d54b4e06cca303e6dff10585c4ed	\N
\.


--
-- Data for Name: items_tags; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.items_tags (id, item_id, name, value, plaintext) FROM stdin;
1	2	\\x4d74610bfb7259efa6796f0522fd8cc97251e663db2e1231a83274e39b9283	\\x5061dbc62cce8be5eaf9f75b8ce3624a8ec9a18c6e76076fb81cb5e2c933f2755fa09f	0
2	2	\\xb8ca0dda9fe28d22282147aa5b939f60663a52827409bf0d039ef57e0b14ff4d7d	\\xc84cb2967b41cc7941dc1f5559e84fde5465d1519c0f1a314b7bd5e144314a2a2f852bb4ab7831b036efcee827c4f907eecf30a6e3700b5a076383fda0e1144c61641e3bb7b256	0
6	5	\\x7a5bbf35c1203a2ee1a52aa849c2a2ddd1a4bb00e1291598fea9889a708f9247c2e3	\\x9a9b21a37d221d53d8f98065e60c9ff1abf7f2c52feffb46af2cfb22cc151d	0
7	5	\\x18b4273f8957c0bc70d5b3a77ad800cd596af1cf3a279a07a236c9e5f466b7dee847	\\x47d61052376ae972b9f8b807e7363f235af7782632bfa1213ad02b4c35f59bc91df8a98e2d5a8e60166391a6120a5ea31a986c32181670de498c039349d3b91e8819b29a3140c6f0	0
8	5	\\x6bfcf090689ed0417e0c2365870741b0398942e2a5f1633f9859273cbf818e6fa521ba65da36e8	\\x5061dbc62cce8be5eaf9f75b8ce3624a8ec9a18c6e76076fb81cb5e2c933f2755fa09f	0
\.


--
-- Data for Name: profiles; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.profiles (id, name, reference, profile_key) FROM stdin;
1	d3de636d-3e2f-4d73-89ae-c7cb902c62f6	\N	\\x71029bee1405f3d78eda63c9d4490a5fba52814d5ac086e0c3b153041e4ff89e7bd1fcaef9f6a6d69ddac77fe3ea23201746ea83ac9a3d196eb19e07939b35d2433c8088f4d61842b3fd7239bf381e16203cbee90487314c87148c421e585fdce51d3233417c297d05eb3b6d383f9fb0f4b4f554cc64f2c5b4d6feaeba09964ebacd09ed672a205493c05e2a8e77af9778172fd2dd4e62e680f993d5ba032caa66a7aa864f7d6ccac7b7df93ff76588a73fb5f1c013e36e9c04b5d6cc1cc14b63d054d1e2b2184ba0e24279707746f7eeeefb2001845831852b548211254d8133da802f4914834823558d69def2e5645de07690f0bf3d36d195470b874103338eb36ed54306943
\.


--
-- Name: items_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.items_id_seq', 5, true);


--
-- Name: items_tags_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.items_tags_id_seq', 8, true);


--
-- Name: profiles_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.profiles_id_seq', 1, true);


--
-- Name: config config_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.config
    ADD CONSTRAINT config_pkey PRIMARY KEY (name);


--
-- Name: items items_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT items_pkey PRIMARY KEY (id);


--
-- Name: items_tags items_tags_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items_tags
    ADD CONSTRAINT items_tags_pkey PRIMARY KEY (id);


--
-- Name: profiles profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT profiles_pkey PRIMARY KEY (id);


--
-- Name: ix_items_tags_item_id; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_items_tags_item_id ON public.items_tags USING btree (item_id);


--
-- Name: ix_items_tags_name_enc; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_items_tags_name_enc ON public.items_tags USING btree (name, substr(value, 1, 12)) WHERE (plaintext = 0);


--
-- Name: ix_items_tags_name_plain; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX ix_items_tags_name_plain ON public.items_tags USING btree (name, value) WHERE (plaintext = 1);


--
-- Name: ix_items_uniq; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_items_uniq ON public.items USING btree (profile_id, kind, category, name);


--
-- Name: ix_profile_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX ix_profile_name ON public.profiles USING btree (name);


--
-- Name: items items_profile_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items
    ADD CONSTRAINT items_profile_id_fkey FOREIGN KEY (profile_id) REFERENCES public.profiles(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: items_tags items_tags_item_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.items_tags
    ADD CONSTRAINT items_tags_item_id_fkey FOREIGN KEY (item_id) REFERENCES public.items(id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

