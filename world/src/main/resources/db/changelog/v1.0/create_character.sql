--liquibase formatted sql

--changeset fireball:create_character
CREATE TABLE characters.character
(
    id          BIGSERIAL PRIMARY KEY,
    account_id  BIGINT    NOT NULL,
    name        VARCHAR(12) NOT NULL,
    race        SMALLINT  NOT NULL,
    char_class  SMALLINT  NOT NULL,
    gender      SMALLINT  NOT NULL,
    skin        SMALLINT  NOT NULL DEFAULT 0,
    face        SMALLINT  NOT NULL DEFAULT 0,
    hair_style  SMALLINT  NOT NULL DEFAULT 0,
    hair_color  SMALLINT  NOT NULL DEFAULT 0,
    facial_hair SMALLINT  NOT NULL DEFAULT 0,
    level       SMALLINT  NOT NULL DEFAULT 1,
    zone        INT       NOT NULL DEFAULT 0,
    map_id      INT       NOT NULL,
    pos_x       REAL      NOT NULL,
    pos_y       REAL      NOT NULL,
    pos_z       REAL      NOT NULL,
    orientation REAL      NOT NULL DEFAULT 0,
    CONSTRAINT uq_character_name UNIQUE (name)
);