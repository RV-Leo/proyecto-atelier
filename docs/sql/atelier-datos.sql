-- =====================================================================
--  Atelier - Datos de ejemplo de la base de negocio "atelier"
-- =====================================================================
--  QUE ES
--    Respaldo SOLO DE DATOS (sin esquema) de la base atelier, para que
--    todo el equipo vea las mismas prendas, materiales y tendencias al
--    levantar el backend.
--
--  REQUISITOS
--    1. Haber creado el rol y las bases: docs/sql/init-postgres.sql
--    2. Haber arrancado el backend UNA vez para que Flyway cree el
--       esquema (V1..V5). Este archivo NO crea tablas.
--    3. Las tablas deben estar vacias. El script se detiene si ya hay
--       prendas, para no duplicar filas ni romper claves.
--
--  USO
--    & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U atelier -d atelier -f docs\sql\atelier-datos.sql
--
--  CONTENIDO: 3 prendas, 2 materiales, 1 tendencia, 1 propuesta de
--  diseno, 1 analisis de viabilidad y 1 fila de ficha tecnica.
--  No incluye usuarios ni contrasenas.
-- =====================================================================

\set ON_ERROR_STOP on
BEGIN;

-- Guardia: evita cargar los datos dos veces sobre la misma base.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM public.prenda) THEN
        RAISE EXCEPTION 'La tabla prenda ya tiene datos. Este archivo es solo para una base vacia.';
    END IF;
END
$$;
--
-- PostgreSQL database dump
--


-- Dumped from database version 18.6
-- Dumped by pg_dump version 18.6

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

--
-- Data for Name: prenda; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.prenda (id, nombre, tipo_prenda, estilo, colores, tela, descripcion, prompt_ia, estado, creado_en, actualizado_en, slug) VALUES (64, 'Vestido de verano andino', 'vestido', 'casual', 'negro', 'viscosa', NULL, NULL, 'BORRADOR', '2026-10-13 13:30:11.515806-05', '2026-10-13 13:30:11.515806-05', 'vestido-de-verano-andino');
INSERT INTO public.prenda (id, nombre, tipo_prenda, estilo, colores, tela, descripcion, prompt_ia, estado, creado_en, actualizado_en, slug) VALUES (65, 'Blusa lino natural', 'blusa', 'boho', 'crudo, terracota', 'lino', 'Blusa holgada de lino con bordados artesanales.', NULL, 'BORRADOR', '2026-10-14 00:14:11.237382-05', '2026-10-14 00:14:11.237382-05', 'blusa-lino-natural');
INSERT INTO public.prenda (id, nombre, tipo_prenda, estilo, colores, tela, descripcion, prompt_ia, estado, creado_en, actualizado_en, slug) VALUES (28, 'Vestido de verano', 'vestido', 'casual', 'negro', 'viscosa', NULL, 'Vestido casual de viscosa negra, corte recto, manga larga, fotografiado sobre fondo de estudio neutro.', 'GENERADA', '2026-10-13 11:00:28.897297-05', '2026-10-13 11:01:52.346652-05', 'vestido-de-verano');


--
-- Data for Name: analisis_viabilidad; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.analisis_viabilidad (id, prenda_id, viable, puntaje, comentario, analizado_en) VALUES (19, 28, false, 0, 'Vestido casual de viscosa negra, corte recto, manga larga, fotografiado sobre fondo de estudio neutro.', '2026-10-13 11:00:54.69146-05');


--
-- Data for Name: material; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.material (id, nombre, tipo, color, unidad, stock, stock_minimo, proveedor, precio_unitario, activo, actualizado_en) VALUES (79, 'Lino natural', 'tela', 'crudo', 'METRO', 120.500, 30.000, 'Textiles Lima', 18.75, true, '2026-10-14 00:14:10.289327-05');
INSERT INTO public.material (id, nombre, tipo, color, unidad, stock, stock_minimo, proveedor, precio_unitario, activo, actualizado_en) VALUES (41, 'Viscosa', 'tela', 'negro', 'METRO', 4.000, 15.000, NULL, NULL, true, '2026-10-13 11:00:28.317494-05');


--
-- Data for Name: alternativa_stock; Type: TABLE DATA; Schema: public; Owner: -
--



--
-- Data for Name: prenda_material; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.prenda_material (id, prenda_id, material_id, cantidad_requerida) VALUES (19, 28, 41, 10.000);


--
-- Data for Name: propuesta_diseno; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.propuesta_diseno (id, prenda_id, imagen_url, prompt_usado, seleccionada, creado_en) VALUES (1, 28, '/imagenes/2026-10-13/5a50be91-2099-43e1-80cf-2466c3fc7da1.png', 'Vestido casual de viscosa negra, corte recto, manga larga, fotografiado sobre fondo de estudio neutro.', true, '2026-10-13 11:01:52.332515-05');


--
-- Data for Name: tendencia; Type: TABLE DATA; Schema: public; Owner: -
--

INSERT INTO public.tendencia (id, titulo, region, fuente, url_origen, imagen_url, colores, estilo, tela, capturado_en) VALUES (25, 'Lino natural y bordados artesanales', 'AMERICA', 'PINTEREST', 'https://www.pinterest.com/pin/ejemplo', 'https://example.com/tendencia.jpg', 'crudo, terracota', 'boho', 'lino', '2026-10-14 00:14:12.393294-05');


--
-- Name: alternativa_stock_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.alternativa_stock_id_seq', 15, true);


--
-- Name: analisis_viabilidad_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.analisis_viabilidad_id_seq', 39, true);


--
-- Name: material_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.material_id_seq', 98, true);


--
-- Name: prenda_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.prenda_id_seq', 77, true);


--
-- Name: prenda_material_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.prenda_material_id_seq', 34, true);


--
-- Name: propuesta_diseno_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.propuesta_diseno_id_seq', 1, true);


--
-- Name: tendencia_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.tendencia_id_seq', 27, true);


--
-- PostgreSQL database dump complete
--


COMMIT;
