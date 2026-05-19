--liquibase formatted sql

--changeset fireball:create_account
CREATE TABLE auth.account
(
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(32) NOT NULL,
    salt        BYTEA       NOT NULL,
    verifier    BYTEA       NOT NULL,
    session_key BYTEA,
    last_ip     INET,
    last_login  TIMESTAMPTZ,
    banned      BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_account_username UNIQUE (username)
);
