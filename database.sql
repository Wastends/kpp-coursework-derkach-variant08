-- Скрипт заново створює навчальну БД і видаляє всі її попередні дані.
SET NAMES utf8mb4;
DROP DATABASE IF EXISTS kpp_water_quality_v08;
CREATE DATABASE kpp_water_quality_v08
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE kpp_water_quality_v08;

CREATE TABLE observation_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(24) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    location VARCHAR(180) NOT NULL,
    water_use VARCHAR(80) NOT NULL,
    sensitivity_score TINYINT UNSIGNED NOT NULL,
    CONSTRAINT chk_point_sensitivity CHECK (sensitivity_score IN (25, 50, 75, 100))
) ENGINE=InnoDB;

CREATE TABLE water_sample (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    point_id BIGINT NOT NULL,
    code VARCHAR(24) NOT NULL UNIQUE,
    sampled_at DATETIME NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'REGISTERED',
    CONSTRAINT chk_sample_state CHECK (state IN ('REGISTERED', 'VALIDATED', 'ANALYZED', 'ARCHIVED')),
    CONSTRAINT fk_sample_point FOREIGN KEY (point_id) REFERENCES observation_point(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE measurement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_id BIGINT NOT NULL,
    parameter_name VARCHAR(80) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    measured_value DECIMAL(12,4) NOT NULL,
    limit_value DECIMAL(12,4) NOT NULL,
    CONSTRAINT uq_measurement_parameter UNIQUE (sample_id, parameter_name),
    CONSTRAINT chk_measurement_value CHECK (measured_value >= 0),
    CONSTRAINT chk_measurement_limit CHECK (limit_value > 0),
    CONSTRAINT fk_measurement_sample FOREIGN KEY (sample_id) REFERENCES water_sample(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

CREATE TABLE sample_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sample_id BIGINT NOT NULL,
    checked_at DATETIME NOT NULL,
    result VARCHAR(16) NOT NULL,
    note VARCHAR(300) NOT NULL,
    CONSTRAINT chk_check_result CHECK (result IN ('PASSED', 'FAILED')),
    CONSTRAINT fk_check_sample FOREIGN KEY (sample_id) REFERENCES water_sample(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB;

-- Умовні демонстраційні значення для перевірки роботи системи.
INSERT INTO observation_point (id, code, name, location, water_use, sensitivity_score) VALUES
    (1, 'PT-01', 'Північний пункт', 'Полтава, навчальна ділянка 1', 'Рекреаційне', 50),
    (2, 'PT-02', 'Південний пункт', 'Полтава, навчальна ділянка 2', 'Питне', 100);

INSERT INTO water_sample (id, point_id, code, sampled_at, state) VALUES
    (1, 1, 'S-001', '2026-11-18 09:00:00', 'ANALYZED'),
    (2, 2, 'S-002', '2026-11-19 10:30:00', 'REGISTERED');

INSERT INTO measurement (id, sample_id, parameter_name, unit, measured_value, limit_value) VALUES
    (1, 1, 'Нітрати', 'мг/л', 15.0000, 10.0000),
    (2, 1, 'Фосфати', 'мг/л', 0.2000, 0.4000),
    (3, 2, 'Нітрати', 'мг/л', 8.0000, 10.0000);

INSERT INTO sample_check (id, sample_id, checked_at, result, note) VALUES
    (1, 1, '2026-11-18 11:30:00', 'PASSED', 'Код проби, одиниці та значення підтверджено.');
