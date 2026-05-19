--liquibase formatted sql

--changeset fireball:create_realm
CREATE TABLE auth.realm
(
    id         SERIAL PRIMARY KEY,
    name       VARCHAR(32)  NOT NULL,
    address    VARCHAR(255) NOT NULL,
    icon       SMALLINT     NOT NULL DEFAULT 0,
    timezone   SMALLINT     NOT NULL DEFAULT 1,
    population REAL         NOT NULL DEFAULT 0.0
);
