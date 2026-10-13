-- Modulo: catalogo web (decision de arquitectura: la web de Django consume esta
-- API en lugar de tener sus propias tablas de producto).
--
-- Django localiza el producto por slug y no por id: su plantilla de detalle
-- construye /catalogo/<slug>/ con get_absolute_url(). Sin slug en esta API la
-- web no podria abrir el detalle de una prenda.

ALTER TABLE prenda ADD COLUMN slug VARCHAR(140);

-- Respaldo de las filas existentes. El regexp reproduce a grandes rasgos el
-- slugify de Django; si el nombre no aporta ningun caracter valido, se usa el
-- id para no dejar la columna vacia.
UPDATE prenda
SET slug = lower(trim(both '-' from regexp_replace(nombre, '[^a-zA-Z0-9]+', '-', 'g')));

UPDATE prenda
SET slug = 'prenda-' || id
WHERE slug IS NULL OR slug = '';

ALTER TABLE prenda ALTER COLUMN slug SET NOT NULL;

CREATE UNIQUE INDEX ux_prenda_slug ON prenda (slug);
