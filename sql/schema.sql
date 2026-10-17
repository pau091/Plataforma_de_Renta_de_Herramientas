-- Script de creación MySQL. Flyway aplica el mismo esquema al arrancar.
-- Fuente: infraestructura/src/main/resources/db/migration/V1__create_initial_schema.sql
--         + V3, V4 y V5.

CREATE DATABASE IF NOT EXISTS renta_herramientas_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE renta_herramientas_db;
