-- =============================================================================
-- TOPICHUB DATABASE SCHEMA - POSTGRESQL (NEON POOLED)
-- Chuẩn CSDL 18 Bảng Cốt Lõi (Đã loại bỏ triệt để bộ môn & chuyên ngành)
-- Quản trị tinh gọn theo Khoa (departments) - Tương thích 1:1 Spring Boot JPA
-- =============================================================================

-- =============================================================================
-- 1. HỆ THỐNG TỔ CHỨC & PHÂN QUYỀN (ORGANIZATION & SECURITY)
-- =============================================================================

-- Bảng 1: roles (Vai trò hệ thống)
CREATE TABLE roles (
    id                         BIGSERIAL PRIMARY KEY,
    code                       VARCHAR(50) NOT NULL UNIQUE,
    name                       VARCHAR(100) NOT NULL
);

-- Bảng 2: users (Tài khoản người dùng)
CREATE TABLE users (
    id                         BIGSERIAL PRIMARY KEY,
    username                   VARCHAR(50) NOT NULL UNIQUE,
    password_hash              VARCHAR(255) NOT NULL,
    full_name                  VARCHAR(100) NOT NULL,
    email                      VARCHAR(100) NOT NULL UNIQUE,
    phone                      VARCHAR(20),
    role_id                    BIGINT REFERENCES roles(id),
    is_active                  BOOLEAN DEFAULT true,
    created_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 3: departments (Khoa / Khoa đào tạo)
CREATE TABLE departments (
    id                         BIGSERIAL PRIMARY KEY,
    code                       VARCHAR(50) NOT NULL UNIQUE,
    name                       VARCHAR(100) NOT NULL,
    description                TEXT,
    is_active                  BOOLEAN DEFAULT true
);

-- =============================================================================
-- 2. HỒ SƠ HỌC THUẬT & THÔNG BÁO (ACADEMIC PROFILES & ANNOUNCEMENTS)
-- =============================================================================

-- Bảng 4: lecturers (Hồ sơ giảng viên - Trực thuộc Khoa)
CREATE TABLE lecturers (
    id                         BIGSERIAL PRIMARY KEY,
    user_id                    BIGINT UNIQUE REFERENCES users(id),
    department_id              BIGINT REFERENCES departments(id),
    lecturer_code              VARCHAR(50) UNIQUE,
    academic_title             VARCHAR(50),
    degree                     VARCHAR(50),
    max_topics_quota           INTEGER DEFAULT 5
);

-- Bảng 5: students (Hồ sơ sinh viên - Trực thuộc Khoa)
CREATE TABLE students (
    id                         BIGSERIAL PRIMARY KEY,
    user_id                    BIGINT UNIQUE REFERENCES users(id),
    department_id              BIGINT REFERENCES departments(id),
    student_code               VARCHAR(50) UNIQUE,
    class_name                 VARCHAR(50),
    academic_year              VARCHAR(50),
    gpa                        NUMERIC(3,2)
);

-- Bảng 6: registration_periods (Đợt đăng ký đề tài 2 giai đoạn)
CREATE TABLE registration_periods (
    id                         BIGSERIAL PRIMARY KEY,
    name                       VARCHAR(200) NOT NULL,
    department_id              BIGINT REFERENCES departments(id),
    type                       VARCHAR(20),
    period_type                VARCHAR(50) DEFAULT 'KLTN',
    academic_year              VARCHAR(50) DEFAULT '2026-2027',
    semester                   INTEGER DEFAULT 1,
    lecturer_proposal_start    TIMESTAMPTZ,
    lecturer_proposal_end      TIMESTAMPTZ,
    student_registration_start TIMESTAMPTZ,
    student_registration_end   TIMESTAMPTZ,
    reviewer_deadline          TIMESTAMPTZ,
    council_defense_date       TIMESTAMPTZ,
    status                     VARCHAR(20) DEFAULT 'ACTIVE',
    is_active                  BOOLEAN DEFAULT true,
    min_students               INTEGER DEFAULT 2,
    max_students               INTEGER DEFAULT 3,
    min_topics_per_lecturer    INTEGER DEFAULT 3,
    lecturer_reg_start         TIMESTAMPTZ,
    lecturer_reg_end           TIMESTAMPTZ,
    student_reg_start          TIMESTAMPTZ,
    student_reg_end            TIMESTAMPTZ,
    review_score_deadline      TIMESTAMPTZ,
    council_date               TIMESTAMPTZ,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 7: announcements (Thông báo hệ thống / đợt)
CREATE TABLE announcements (
    event_key                  VARCHAR(255) UNIQUE,
    id                         BIGSERIAL PRIMARY KEY,
    department_id              BIGINT REFERENCES departments(id),
    period_id                  BIGINT REFERENCES registration_periods(id),
    author_id                  BIGINT REFERENCES users(id),
    title                      VARCHAR(255) NOT NULL,
    content                    TEXT NOT NULL,
    category                   VARCHAR(50) DEFAULT 'GENERAL',
    attachment_path            VARCHAR(500),
    is_pinned                  BOOLEAN DEFAULT false,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. HỘI ĐỒNG BẢO VỆ & ĐÁNH GIÁ (COUNCILS & EVALUATIONS)
-- =============================================================================

-- Bảng 8: councils (Hội đồng chấm bảo vệ đề tài - Trực thuộc Khoa)
CREATE TABLE councils (
    id                         BIGSERIAL PRIMARY KEY,
    code                       VARCHAR(50),
    name                       VARCHAR(200) NOT NULL,
    department_id              BIGINT REFERENCES departments(id),
    period_id                  BIGINT REFERENCES registration_periods(id),
    chairman_id                BIGINT REFERENCES lecturers(id),
    secretary_id               BIGINT REFERENCES lecturers(id),
    location                   VARCHAR(255),
    defense_date               TIMESTAMPTZ,
    council_date               TIMESTAMPTZ,
    status                     VARCHAR(20) DEFAULT 'ACTIVE',
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 9: council_members (Thành viên hội đồng - 3 đến 5 giảng viên, loại bỏ GVHD)
CREATE TABLE council_members (
    id                         BIGSERIAL PRIMARY KEY,
    council_id                 BIGINT REFERENCES councils(id) ON DELETE CASCADE,
    lecturer_id                BIGINT REFERENCES lecturers(id),
    role                       VARCHAR(20) DEFAULT 'MEMBER',
    council_role               VARCHAR(50) DEFAULT 'MEMBER',
    joined_at                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. ĐỀ TÀI & QUẢN LÝ NHÓM SINH VIÊN (TOPICS & STUDENT GROUPS)
-- =============================================================================

-- Bảng 10: topics (Đề tài tốt nghiệp / khóa luận - Bảng trung tâm)
CREATE TABLE topics (
    id                         BIGSERIAL PRIMARY KEY,
    code                       VARCHAR(50),
    title                      VARCHAR(255) NOT NULL,
    description                TEXT,
    requirements               TEXT,
    department_id              BIGINT REFERENCES departments(id),
    period_id                  BIGINT REFERENCES registration_periods(id),
    supervisor_id              BIGINT REFERENCES lecturers(id),
    co_supervisor_id           BIGINT REFERENCES lecturers(id),
    reviewer_id                BIGINT REFERENCES lecturers(id),
    council_id                 BIGINT REFERENCES councils(id),
    min_students               INTEGER DEFAULT 1,
    max_students               INTEGER DEFAULT 3,
    status                     VARCHAR(20) DEFAULT 'PENDING',
    rejection_reason           TEXT,
    academic_year              VARCHAR(50),
    semester                   VARCHAR(20),
    is_finalized               BOOLEAN DEFAULT false,
    final_score                DOUBLE PRECISION,
    advisor_score              DOUBLE PRECISION,
    reviewer_score             DOUBLE PRECISION,
    council_score              DOUBLE PRECISION,
    version                    BIGINT DEFAULT 0,
    created_at                 TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 11: student_groups (Nhóm sinh viên thực hiện đề tài)
CREATE TABLE student_groups (
    id                         BIGSERIAL PRIMARY KEY,
    group_code                 VARCHAR(50),
    group_name                 VARCHAR(255),
    name                       VARCHAR(100),
    period_id                  BIGINT REFERENCES registration_periods(id),
    leader_id                  BIGINT REFERENCES students(id),
    leader_student_id          BIGINT REFERENCES students(id),
    topic_id                   BIGINT REFERENCES topics(id),
    status                     VARCHAR(20) DEFAULT 'ACTIVE',
    version                    BIGINT DEFAULT 0,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 12: group_members (Thành viên nhóm - Ràng buộc 3 đến 5 sinh viên)
CREATE TABLE group_members (
    id                         BIGSERIAL PRIMARY KEY,
    group_id                   BIGINT REFERENCES student_groups(id) ON DELETE CASCADE,
    student_id                 BIGINT REFERENCES students(id),
    role_in_group              VARCHAR(50) DEFAULT 'MEMBER',
    joined_at                  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 13: topic_registrations (Đơn đăng ký đề tài của nhóm sinh viên)
CREATE TABLE topic_registrations (
    id                         BIGSERIAL PRIMARY KEY,
    topic_id                   BIGINT REFERENCES topics(id),
    group_id                   BIGINT REFERENCES student_groups(id),
    student_id                 BIGINT REFERENCES students(id),
    registered_by_student_id   BIGINT REFERENCES students(id),
    status                     VARCHAR(20) DEFAULT 'PENDING',
    registered_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_at                TIMESTAMP
);

-- Bảng 14: group_invitations (Lời mời tham gia nhóm sinh viên)
CREATE TABLE group_invitations (
    id                         BIGSERIAL PRIMARY KEY,
    group_id                   BIGINT REFERENCES student_groups(id) ON DELETE CASCADE,
    student_id                 BIGINT REFERENCES students(id),
    invited_by_student_id      BIGINT REFERENCES students(id),
    status                     VARCHAR(50) DEFAULT 'INVITED',
    rejection_reason           TEXT,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    responded_at               TIMESTAMPTZ
);

-- Bảng 15: group_leave_requests (Yêu cầu rời nhóm của sinh viên)
CREATE TABLE group_leave_requests (
    id                         BIGSERIAL PRIMARY KEY,
    group_id                   BIGINT REFERENCES student_groups(id) ON DELETE CASCADE,
    student_id                 BIGINT REFERENCES students(id),
    reason                     TEXT,
    status                     VARCHAR(50) DEFAULT 'PENDING',
    resolved_by_lecturer_id    BIGINT REFERENCES lecturers(id),
    rejection_reason           TEXT,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    resolved_at                TIMESTAMPTZ
);

-- Bảng 16: group_dissolve_requests (Yêu cầu giải tán nhóm của nhóm trưởng)
CREATE TABLE group_dissolve_requests (
    id                         BIGSERIAL PRIMARY KEY,
    group_id                   BIGINT REFERENCES student_groups(id) ON DELETE CASCADE,
    requested_by_student_id    BIGINT REFERENCES students(id),
    topic_id                   BIGINT REFERENCES topics(id),
    reason                     TEXT,
    status                     VARCHAR(50) DEFAULT 'PENDING',
    resolved_by_lecturer_id    BIGINT REFERENCES lecturers(id),
    rejection_reason           TEXT,
    created_at                 TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    resolved_at                TIMESTAMPTZ
);

-- =============================================================================
-- 5. TIẾN ĐỘ, BÁO CÁO & ĐÁNH GIÁ (SUBMISSIONS & EVALUATIONS)
-- =============================================================================

-- Bảng 17: submissions (Báo cáo tiến độ đề tài của nhóm)
CREATE TABLE submissions (
    id                         BIGSERIAL PRIMARY KEY,
    topic_id                   BIGINT REFERENCES topics(id),
    group_id                   BIGINT REFERENCES student_groups(id),
    submitted_by_student_id    BIGINT REFERENCES students(id),
    title                      VARCHAR(255),
    submission_round           VARCHAR(50),
    file_path                  TEXT,
    original_file_name         VARCHAR(255),
    file_size                  BIGINT,
    content_type               VARCHAR(100),
    grade                      NUMERIC(4,2),
    feedback                   TEXT,
    submitted_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 18: evaluations (Chấm điểm đánh giá - Giảng viên phản biện & Hội đồng)
CREATE TABLE evaluations (
    id                         BIGSERIAL PRIMARY KEY,
    topic_id                   BIGINT REFERENCES topics(id),
    group_id                   BIGINT REFERENCES student_groups(id),
    lecturer_id                BIGINT REFERENCES lecturers(id),
    evaluation_type            VARCHAR(50),
    score                      DOUBLE PRECISION,
    comments                   TEXT,
    is_submitted               BOOLEAN DEFAULT false,
    evaluated_at               TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- TỐI ƯU HÓA CHỈ MỤC (INDEXING FOR QUERY OPTIMIZATION)
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_lecturers_department_id ON lecturers(department_id);
CREATE INDEX IF NOT EXISTS idx_students_department_id ON students(department_id);
CREATE INDEX IF NOT EXISTS idx_topics_period_id ON topics(period_id);
CREATE INDEX IF NOT EXISTS idx_topics_department_id ON topics(department_id);
CREATE INDEX IF NOT EXISTS idx_topics_supervisor_id ON topics(supervisor_id);
CREATE INDEX IF NOT EXISTS idx_topics_status ON topics(status);
CREATE INDEX IF NOT EXISTS idx_councils_department_id ON councils(department_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_period_id ON student_groups(period_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_topic_id ON student_groups(topic_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_student_id ON group_members(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_group_id ON submissions(group_id);
CREATE INDEX IF NOT EXISTS idx_submissions_topic_id ON submissions(topic_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_topic_id ON evaluations(topic_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_lecturer_id ON evaluations(lecturer_id);
