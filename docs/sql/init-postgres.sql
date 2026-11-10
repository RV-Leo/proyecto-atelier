-- =====================================================================
--  Atelier - Inicialización de PostgreSQL (SOLO DESARROLLO LOCAL)
-- =====================================================================
--  Ejecutar como superusuario (postgres):
--
--      & "C:\Program Files\PostgreSQL\18\bin\psql.exe" -U postgres -f docs\sql\init-postgres.sql
--
--  Crea:
--    rol   : atelier
--    base  : atelier      -> datos de negocio (los usa SOLO Spring Boot)
--    base  : atelier_web  -> tablas propias de Django (auth, sessions, admin)
--
--  Ninguna de las dos es SQLite.
-- =====================================================================

-- Rol de la aplicación (idempotente: crea o restablece la contraseña)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'atelier') THEN
        CREATE ROLE atelier WITH LOGIN PASSWORD 'atelier';
    ELSE
        ALTER ROLE atelier WITH LOGIN PASSWORD 'atelier';
    END IF;
END
$$;

-- Base de datos de negocio: dueño = Spring Boot
SELECT 'CREATE DATABASE atelier OWNER atelier ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'atelier')\gexec

-- Base de datos interna de Django: dueño = aplicación web
SELECT 'CREATE DATABASE atelier_web OWNER atelier ENCODING ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'atelier_web')\gexec
