-- =========================================================================
-- DATABASE SCHEMA V3: PERFORMANCE INDEXES
-- =========================================================================
-- Thêm indexes trên foreign keys và status columns để tối ưu hóa queries

-- Indexes trên bảng Topics
CREATE INDEX IF NOT EXISTS idx_topics_period_id ON topics(period_id);
CREATE INDEX IF NOT EXISTS idx_topics_major_id ON topics(major_id);
CREATE INDEX IF NOT EXISTS idx_topics_supervisor_id ON topics(supervisor_id);
CREATE INDEX IF NOT EXISTS idx_topics_co_supervisor_id ON topics(co_supervisor_id);
CREATE INDEX IF NOT EXISTS idx_topics_reviewer_id ON topics(reviewer_id);
CREATE INDEX IF NOT EXISTS idx_topics_council_id ON topics(council_id);
CREATE INDEX IF NOT EXISTS idx_topics_status ON topics(status);
CREATE INDEX IF NOT EXISTS idx_topics_academic_year ON topics(academic_year);

-- Indexes trên bảng Student Groups
CREATE INDEX IF NOT EXISTS idx_student_groups_period_id ON student_groups(period_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_leader_id ON student_groups(leader_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_topic_id ON student_groups(topic_id);
CREATE INDEX IF NOT EXISTS idx_student_groups_status ON student_groups(status);

-- Indexes trên bảng Group Members
CREATE INDEX IF NOT EXISTS idx_group_members_group_id ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_group_members_student_id ON group_members(student_id);

-- Indexes trên bảng Registrations và Submissions
CREATE INDEX IF NOT EXISTS idx_topic_registrations_topic_id ON topic_registrations(topic_id);
CREATE INDEX IF NOT EXISTS idx_topic_registrations_student_id ON topic_registrations(student_id);
CREATE INDEX IF NOT EXISTS idx_topic_registrations_status ON topic_registrations(status);

CREATE INDEX IF NOT EXISTS idx_submissions_topic_id ON submissions(topic_id);
CREATE INDEX IF NOT EXISTS idx_submissions_group_id ON submissions(group_id);
CREATE INDEX IF NOT EXISTS idx_submissions_student_id ON submissions(student_id);
CREATE INDEX IF NOT EXISTS idx_submissions_submitted_by_student_id ON submissions(submitted_by_student_id);

-- Indexes trên bảng Evaluations
CREATE INDEX IF NOT EXISTS idx_evaluations_topic_id ON evaluations(topic_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_group_id ON evaluations(group_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_lecturer_id ON evaluations(lecturer_id);
CREATE INDEX IF NOT EXISTS idx_evaluations_evaluation_type ON evaluations(evaluation_type);

-- Indexes trên bảng Users, Students, Lecturers
CREATE INDEX IF NOT EXISTS idx_users_role_id ON users(role_id);
CREATE INDEX IF NOT EXISTS idx_students_user_id ON students(user_id);
CREATE INDEX IF NOT EXISTS idx_students_major_id ON students(major_id);
CREATE INDEX IF NOT EXISTS idx_lecturers_user_id ON lecturers(user_id);
CREATE INDEX IF NOT EXISTS idx_lecturers_department_id ON lecturers(department_id);

-- Indexes trên bảng Announcements
CREATE INDEX IF NOT EXISTS idx_announcements_department_id ON announcements(department_id);
CREATE INDEX IF NOT EXISTS idx_announcements_period_id ON announcements(period_id);
CREATE INDEX IF NOT EXISTS idx_announcements_author_id ON announcements(author_id);
CREATE INDEX IF NOT EXISTS idx_announcements_is_pinned ON announcements(is_pinned);

-- Indexes trên bảng Councils
CREATE INDEX IF NOT EXISTS idx_councils_period_id ON councils(period_id);
CREATE INDEX IF NOT EXISTS idx_councils_department_id ON councils(department_id);
CREATE INDEX IF NOT EXISTS idx_council_members_council_id ON council_members(council_id);
CREATE INDEX IF NOT EXISTS idx_council_members_lecturer_id ON council_members(lecturer_id);

-- Indexes trên bảng Registration Periods
CREATE INDEX IF NOT EXISTS idx_registration_periods_department_id ON registration_periods(department_id);
CREATE INDEX IF NOT EXISTS idx_registration_periods_status ON registration_periods(status);
