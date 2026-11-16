-- Corrige dos fallas detectadas al conectar las entidades JPA con el esquema:
--
-- 1. Los enums en Java se persisten con @Enumerated(STRING), es decir con el
--    nombre en MAYUSCULAS (METRO, EUROPA, BORRADOR...), pero los CHECK creados
--    en V1-V3 esperaban minusculas. Todo INSERT habria fallado.
-- 2. No cambia datos: solo se ajustan las restricciones.

ALTER TABLE material DROP CONSTRAINT IF EXISTS ck_material_unidad;
ALTER TABLE material ADD CONSTRAINT ck_material_unidad
    CHECK (unidad IN ('METRO', 'UNIDAD', 'KILOGRAMO', 'ROLLO'));

ALTER TABLE prenda DROP CONSTRAINT IF EXISTS ck_prenda_estado;
ALTER TABLE prenda ADD CONSTRAINT ck_prenda_estado
    CHECK (estado IN ('BORRADOR', 'GENERANDO', 'GENERADA', 'ANALIZADA'));

ALTER TABLE tendencia DROP CONSTRAINT IF EXISTS ck_tendencia_region;
ALTER TABLE tendencia ADD CONSTRAINT ck_tendencia_region
    CHECK (region IN ('EUROPA', 'AMERICA', 'GLOBAL'));

ALTER TABLE tendencia DROP CONSTRAINT IF EXISTS ck_tendencia_fuente;
ALTER TABLE tendencia ADD CONSTRAINT ck_tendencia_fuente
    CHECK (fuente IN ('PINTEREST', 'TIENDA_MARCA', 'OTRO'));
