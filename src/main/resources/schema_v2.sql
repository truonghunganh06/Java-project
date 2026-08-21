-- =========================================================================
-- DATABASE SCHEMA V2: TOPIC MANAGEMENT SYSTEM (KHOA CNTT)
-- Hỗ trợ đầy đủ: Đợt đăng ký, 2 GVHD, GVPB, Nhóm SV (max 3), Hội đồng 3-5 GV, Chấm điểm, Bảng tin
-- =========================================================================

-- 1. Bảng Đợt Đăng Ký (Registration Periods)
CREATE TABLE IF NOT EXISTS registration_periods (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    period_type VARCHAR(50) NOT NULL, -- MON_HOC, NCKH, TLCN, KLTN
    academic_year VARCHAR(50) NOT NULL DEFAULT '2026-2027',
    semester INT NOT NULL DEFAULT 1,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    lecturer_proposal_start TIMESTAMPTZ,
    lecturer_proposal_end TIMESTAMPTZ,
    student_registration_start TIMESTAMPTZ,
    student_registration_end TIMESTAMPTZ,
    reviewer_deadline TIMESTAMPTZ,
    council_defense_date TIMESTAMPTZ,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN', -- DRAFT, OPEN, IN_PROGRESS, COMPLETED, CLOSED
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. Bảng Hội Đồng Bảo Vệ & Phản Biện (Councils)
CREATE TABLE IF NOT EXISTS councils (
    id BIGSERIAL PRIMARY KEY,
    period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    code VARCHAR(50) UNIQUE,
    name VARCHAR(255) NOT NULL,
    defense_date TIMESTAMPTZ,
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ESTABLISHED', -- ESTABLISHED, IN_PROGRESS, COMPLETED
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. Bảng Thành Viên Hội Đồng (Council Members: 3 - 5 GV)
CREATE TABLE IF NOT EXISTS council_members (
    id BIGSERIAL PRIMARY KEY,
    council_id BIGINT NOT NULL REFERENCES councils(id) ON DELETE CASCADE,
    lecturer_id BIGINT NOT NULL REFERENCES lecturers(id) ON DELETE CASCADE,
    council_role VARCHAR(50) NOT NULL DEFAULT 'MEMBER', -- PRESIDENT, SECRETARY, MEMBER
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. Bổ sung các cột mới cho Bảng Đề Tài (Topics)
ALTER TABLE topics ADD COLUMN IF NOT EXISTS period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL;
ALTER TABLE topics ADD COLUMN IF NOT EXISTS co_supervisor_id BIGINT REFERENCES lecturers(id) ON DELETE SET NULL;
ALTER TABLE topics ADD COLUMN IF NOT EXISTS reviewer_id BIGINT REFERENCES lecturers(id) ON DELETE SET NULL;
ALTER TABLE topics ADD COLUMN IF NOT EXISTS council_id BIGINT REFERENCES councils(id) ON DELETE SET NULL;
ALTER TABLE topics ADD COLUMN IF NOT EXISTS is_finalized BOOLEAN DEFAULT FALSE;
ALTER TABLE topics ADD COLUMN IF NOT EXISTS final_score DOUBLE PRECISION;

-- 5. Bảng Nhóm Sinh Viên (Student Groups)
CREATE TABLE IF NOT EXISTS student_groups (
    id BIGSERIAL PRIMARY KEY,
    period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL,
    group_code VARCHAR(50) UNIQUE NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    leader_id BIGINT REFERENCES students(id) ON DELETE SET NULL,
    topic_id BIGINT REFERENCES topics(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT', -- DRAFT, REGISTERED, APPROVED, REJECTED, COMPLETED
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 6. Bảng Thành Viên Nhóm (Group Members: max 3 SV)
CREATE TABLE IF NOT EXISTS group_members (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL REFERENCES student_groups(id) ON DELETE CASCADE,
    student_id BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    role_in_group VARCHAR(50) NOT NULL DEFAULT 'MEMBER', -- LEADER, MEMBER
    joined_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_student_group UNIQUE(group_id, student_id)
);

-- 7. Bảng Đánh Giá & Điểm Số (Evaluations)
CREATE TABLE IF NOT EXISTS evaluations (
    id BIGSERIAL PRIMARY KEY,
    topic_id BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    group_id BIGINT REFERENCES student_groups(id) ON DELETE SET NULL,
    lecturer_id BIGINT NOT NULL REFERENCES lecturers(id) ON DELETE CASCADE,
    evaluation_type VARCHAR(50) NOT NULL, -- SUPERVISOR, REVIEWER, COUNCIL
    score DOUBLE PRECISION,
    comments TEXT,
    is_submitted BOOLEAN DEFAULT FALSE,
    evaluated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 8. Bảng Bảng Tin & Thông Báo (Announcements)
CREATE TABLE IF NOT EXISTS announcements (
    id BIGSERIAL PRIMARY KEY,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    period_id BIGINT REFERENCES registration_periods(id) ON DELETE SET NULL,
    author_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(50) DEFAULT 'GENERAL', -- GENERAL, PERIOD, SCHEDULE, URGENT
    attachment_path VARCHAR(500),
    is_pinned BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 9. Bổ sung các cột mới cho Bảng Báo Cáo Tiến Độ (Submissions)
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS group_id BIGINT REFERENCES student_groups(id) ON DELETE SET NULL;
ALTER TABLE submissions ADD COLUMN IF NOT EXISTS submitted_by_student_id BIGINT REFERENCES students(id) ON DELETE SET NULL;
