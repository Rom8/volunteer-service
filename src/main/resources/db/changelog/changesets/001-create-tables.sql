--liquibase formatted sql

--changeset rom8:001-create-location
-- Иерархическая структура локаций (самоссылающаяся таблица)
CREATE TABLE location
(
    id            BIGSERIAL    PRIMARY KEY,
    name          VARCHAR(255) NOT NULL UNIQUE,
    parent_loc_id BIGINT       REFERENCES location (id) ON DELETE SET NULL,
    location_kind VARCHAR(10)  NOT NULL DEFAULT 'PARENT'
                                   CHECK (location_kind IN ('PARENT', 'CHILD')),
    create_date   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date   TIMESTAMPTZ
);
--rollback DROP TABLE location;


--changeset rom8:002-create-volunteer
-- Основная таблица волонтёров
CREATE TABLE volunteer
(
    id                  BIGSERIAL    PRIMARY KEY,
    user_id             VARCHAR(100) NOT NULL UNIQUE,
    first_name          VARCHAR(100) NOT NULL,
    family_name         VARCHAR(100) NOT NULL,
    patronymic          VARCHAR(100),
    status              VARCHAR(20)  NOT NULL DEFAULT 'FREE'
                                         CHECK (status IN ('FREE', 'ASSIGNED_TASK')),
    location_id         BIGINT       REFERENCES location (id) ON DELETE SET NULL,
    current_incident_id UUID,
    create_date         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date         TIMESTAMPTZ
);
--rollback DROP TABLE volunteer;


--changeset rom8:003-create-contact-info
-- Контактные данные волонтёра (телефон / email)
CREATE TABLE contact_info
(
    id           BIGSERIAL    PRIMARY KEY,
    contact      VARCHAR(255) NOT NULL UNIQUE,
    contact_type VARCHAR(10)  NOT NULL
                                  CHECK (contact_type IN ('PHONE', 'EMAIL')),
    volunteer_id BIGINT       REFERENCES volunteer (id) ON DELETE CASCADE,
    create_date  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date  TIMESTAMPTZ
);
--rollback DROP TABLE contact_info;
