-- Braculink schema — hand-written DDL, no ORM generation.
-- ENGINE=InnoDB everywhere so foreign keys are actually enforced (MyISAM ignores them).

CREATE TABLE IF NOT EXISTS course_section (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    section_id           BIGINT NOT NULL,
    course_code          VARCHAR(20) NOT NULL,
    course_name          VARCHAR(150),
    course_type          VARCHAR(20),
    section_name         VARCHAR(10),
    faculties            VARCHAR(100),
    room_name            VARCHAR(50),
    capacity             INT,
    consumed_seat        INT,
    semester_session_id  INT NOT NULL,
    class_schedules      JSON,
    lab_section_id       BIGINT,
    lab_faculties        VARCHAR(100),
    lab_schedules        JSON,
    last_synced_at       DATETIME,
    UNIQUE KEY uq_section_semester (section_id, semester_session_id),
    INDEX idx_course_code (course_code)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id     VARCHAR(20) NOT NULL UNIQUE,
    full_name      VARCHAR(120) NOT NULL,
    bracu_email    VARCHAR(120) NOT NULL UNIQUE,
    password_hash  VARCHAR(200) NOT NULL,
    phone_number   VARCHAR(20),
    phone_public   BOOLEAN NOT NULL DEFAULT FALSE,
    fb_profile_url VARCHAR(255),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS enrollment (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    section_id           BIGINT NOT NULL,
    semester_session_id  INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (section_id) REFERENCES course_section(id),
    UNIQUE KEY uq_user_section (user_id, section_id, semester_session_id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS swap_group (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_code  VARCHAR(20) NOT NULL,
    status       VARCHAR(20) NOT NULL,
    created_at   DATETIME NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS swap_request (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id              BIGINT NOT NULL,
    course_code          VARCHAR(20) NOT NULL,
    current_section_id   BIGINT NOT NULL,
    desired_section_id   BIGINT NOT NULL,
    status               VARCHAR(20) NOT NULL,
    confirmed            BOOLEAN NOT NULL DEFAULT FALSE,
    group_id             BIGINT NULL,
    created_at           DATETIME NOT NULL,
    responded_at         DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (current_section_id) REFERENCES course_section(id),
    FOREIGN KEY (desired_section_id) REFERENCES course_section(id),
    FOREIGN KEY (group_id) REFERENCES swap_group(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    type        VARCHAR(50),
    payload     TEXT,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES user(id)
    -- "read" is a reserved word in MySQL, so the column is is_read
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS friendship (
    requester_id  BIGINT NOT NULL,
    addressee_id  BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL,
    created_at    DATETIME NOT NULL,
    PRIMARY KEY (requester_id, addressee_id),
    FOREIGN KEY (requester_id) REFERENCES user(id),
    FOREIGN KEY (addressee_id) REFERENCES user(id)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS otp (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(120) NOT NULL,
    code        VARCHAR(10) NOT NULL,
    expires_at  DATETIME NOT NULL,
    INDEX idx_otp_email (email)
) ENGINE=InnoDB;
