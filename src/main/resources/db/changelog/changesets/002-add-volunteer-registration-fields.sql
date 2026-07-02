--liquibase formatted sql

--changeset rom8:004-add-volunteer-registration-fields
ALTER TABLE volunteer
    ADD COLUMN gender VARCHAR(10) NOT NULL
        CHECK (gender IN ('MALE', 'FEMALE')),
    ADD COLUMN birth_date DATE NOT NULL;
--rollback ALTER TABLE volunteer DROP COLUMN birth_date, DROP COLUMN gender;
