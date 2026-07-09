CREATE DATABASE IF NOT EXISTS course_collab DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE course_collab;
SET NAMES utf8mb4;

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS audit_history;
DROP TABLE IF EXISTS showcase;
DROP TABLE IF EXISTS discussion_post;
DROP TABLE IF EXISTS project_member;
DROP TABLE IF EXISTS project_group;
DROP TABLE IF EXISTS assignment_submission;
DROP TABLE IF EXISTS assignment;
DROP TABLE IF EXISTS course_resource;
DROP TABLE IF EXISTS course_notice;
DROP TABLE IF EXISTS course_member;
DROP TABLE IF EXISTS file_gc_queue;
DROP TABLE IF EXISTS file_reference;
DROP TABLE IF EXISTS file_metadata;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE sys_user (
  id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  username VARCHAR(50) NOT NULL COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt密码',
  real_name VARCHAR(50) NOT NULL COMMENT '真实姓名',
  role_code VARCHAR(20) NOT NULL COMMENT 'ADMIN/TEACHER/TA/STUDENT',
  email VARCHAR(100) COMMENT '邮箱',
  phone VARCHAR(30) COMMENT '手机号',
  gender CHAR(1) NOT NULL DEFAULT 'U' COMMENT 'M/F/U',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0禁用 1启用',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_sys_user_username UNIQUE (username),
  CONSTRAINT ck_sys_user_role CHECK (role_code IN ('ADMIN','TEACHER','TA','STUDENT')),
  CONSTRAINT ck_sys_user_gender CHECK (gender IN ('M','F','U')),
  CONSTRAINT ck_sys_user_status CHECK (status IN (0,1))
) COMMENT='系统用户';

CREATE TABLE course (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_code VARCHAR(50) NOT NULL COMMENT '课程编号',
  course_name VARCHAR(100) NOT NULL COMMENT '课程名称',
  teacher_id BIGINT NOT NULL COMMENT '教师ID',
  teacher_name VARCHAR(50) COMMENT '教师姓名冗余',
  description VARCHAR(500),
  credit DECIMAL(3,1) NOT NULL,
  hours INT NOT NULL,
  max_students INT NOT NULL DEFAULT 60,
  current_students INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停开 1正常',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_course_code UNIQUE (course_code),
  CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user(id),
  CONSTRAINT ck_course_credit CHECK (credit BETWEEN 0.5 AND 10.0),
  CONSTRAINT ck_course_hours CHECK (hours BETWEEN 1 AND 200),
  CONSTRAINT ck_course_capacity CHECK (max_students >= current_students AND max_students > 0),
  INDEX idx_course_teacher (teacher_id),
  INDEX idx_course_name (course_name)
) COMMENT='课程';

CREATE TABLE course_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(20) NOT NULL COMMENT 'TEACHER/TA/STUDENT',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_course_member UNIQUE (course_id, user_id, deleted),
  CONSTRAINT fk_course_member_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_course_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  CONSTRAINT ck_course_member_role CHECK (role_code IN ('TEACHER','TA','STUDENT'))
) COMMENT='课程成员';

CREATE TABLE file_metadata (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  original_name VARCHAR(255) NOT NULL,
  storage_name VARCHAR(255) NOT NULL,
  storage_path VARCHAR(500) NOT NULL,
  content_type VARCHAR(100),
  content_hash VARCHAR(64),
  size_bytes BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  biz_type VARCHAR(50) NOT NULL DEFAULT 'course',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id),
  CONSTRAINT ck_file_size CHECK (size_bytes >= 0),
  INDEX idx_file_uploader (uploader_id),
  INDEX idx_file_biz_type (biz_type),
  INDEX idx_file_content_hash (content_hash, size_bytes, deleted, status)
) COMMENT='文件元数据';

CREATE TABLE file_reference (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_id BIGINT NOT NULL,
  owner_type VARCHAR(50) NOT NULL COMMENT 'COURSE_RESOURCE/ASSIGNMENT/SUBMISSION/SHOWCASE',
  owner_id BIGINT NOT NULL COMMENT '业务记录ID',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '0停用 1有效',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除，删除后触发GC候选入队',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_reference_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  CONSTRAINT ck_file_reference_owner CHECK (owner_type IN ('COURSE_RESOURCE','ASSIGNMENT','SUBMISSION','SHOWCASE')),
  CONSTRAINT ck_file_reference_status CHECK (status IN (0,1)),
  INDEX idx_file_reference_file (file_id, deleted, status),
  INDEX idx_file_reference_owner (owner_type, owner_id, deleted),
  INDEX idx_file_reference_lookup (owner_type, owner_id, file_id, deleted)
) COMMENT='文件业务引用';

CREATE TABLE file_gc_queue (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  file_id BIGINT NOT NULL,
  source_reference_id BIGINT,
  reason VARCHAR(50) NOT NULL DEFAULT 'REFERENCE_RELEASED',
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1处理中 2已释放 3仍被引用 4失败',
  attempts INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000),
  next_retry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  processed_time DATETIME,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_file_gc_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  CONSTRAINT fk_file_gc_reference FOREIGN KEY (source_reference_id) REFERENCES file_reference(id),
  CONSTRAINT ck_file_gc_status CHECK (status IN (0,1,2,3,4)),
  CONSTRAINT ck_file_gc_attempts CHECK (attempts >= 0),
  INDEX idx_file_gc_status_retry (status, next_retry_time),
  INDEX idx_file_gc_file (file_id, status)
) COMMENT='文件回收队列';

CREATE TABLE course_notice (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(100) NOT NULL,
  content TEXT NOT NULL,
  publisher_id BIGINT NOT NULL,
  pinned TINYINT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_notice_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_notice_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user(id),
  INDEX idx_notice_course_time (course_id, create_time)
) COMMENT='课程通知';

CREATE TABLE course_resource (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  file_id BIGINT,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(50) NOT NULL,
  tags VARCHAR(200),
  description VARCHAR(500),
  uploader_id BIGINT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_resource_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_resource_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  CONSTRAINT fk_resource_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user(id),
  INDEX idx_resource_course_category (course_id, category),
  INDEX idx_resource_tags (tags)
) COMMENT='课程资源';

CREATE TABLE assignment (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  description TEXT,
  file_id BIGINT,
  due_time DATETIME,
  total_score DECIMAL(5,2) NOT NULL DEFAULT 100.00,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_assignment_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_assignment_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  CONSTRAINT ck_assignment_score CHECK (total_score > 0),
  INDEX idx_assignment_course_due (course_id, due_time)
) COMMENT='作业';

CREATE TABLE assignment_submission (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  assignment_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  file_id BIGINT,
  content TEXT,
  score DECIMAL(5,2),
  feedback VARCHAR(1000),
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0已提交 1已批改',
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_assignment_student UNIQUE (assignment_id, student_id, deleted),
  CONSTRAINT fk_submission_assignment FOREIGN KEY (assignment_id) REFERENCES assignment(id),
  CONSTRAINT fk_submission_student FOREIGN KEY (student_id) REFERENCES sys_user(id),
  CONSTRAINT fk_submission_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  CONSTRAINT ck_submission_score CHECK (score IS NULL OR score BETWEEN 0 AND 100),
  INDEX idx_submission_assignment (assignment_id)
) COMMENT='作业提交';

CREATE TABLE project_group (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  topic VARCHAR(200) NOT NULL,
  leader_id BIGINT NOT NULL,
  max_members INT NOT NULL DEFAULT 5,
  current_members INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_project_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_project_leader FOREIGN KEY (leader_id) REFERENCES sys_user(id),
  CONSTRAINT ck_project_members CHECK (max_members >= current_members AND max_members > 0),
  INDEX idx_project_course (course_id)
) COMMENT='课程项目组';

CREATE TABLE project_member (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_name VARCHAR(30) NOT NULL DEFAULT '成员',
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT uk_project_member UNIQUE (group_id, user_id, deleted),
  CONSTRAINT fk_project_member_group FOREIGN KEY (group_id) REFERENCES project_group(id),
  CONSTRAINT fk_project_member_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
  INDEX idx_project_member_user (user_id)
) COMMENT='项目组成员';

CREATE TABLE discussion_post (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  parent_id BIGINT,
  author_id BIGINT NOT NULL,
  title VARCHAR(120) NOT NULL,
  content TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_discussion_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_discussion_group FOREIGN KEY (group_id) REFERENCES project_group(id),
  CONSTRAINT fk_discussion_parent FOREIGN KEY (parent_id) REFERENCES discussion_post(id),
  CONSTRAINT fk_discussion_author FOREIGN KEY (author_id) REFERENCES sys_user(id),
  INDEX idx_discussion_course (course_id, create_time)
) COMMENT='课程讨论';

CREATE TABLE showcase (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  file_id BIGINT,
  title VARCHAR(120) NOT NULL,
  summary VARCHAR(1000),
  link_url VARCHAR(255),
  status TINYINT NOT NULL DEFAULT 1,
  deleted TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_showcase_course FOREIGN KEY (course_id) REFERENCES course(id),
  CONSTRAINT fk_showcase_group FOREIGN KEY (group_id) REFERENCES project_group(id),
  CONSTRAINT fk_showcase_file FOREIGN KEY (file_id) REFERENCES file_metadata(id),
  INDEX idx_showcase_course (course_id)
) COMMENT='课程成果展示';

CREATE TABLE audit_history (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  table_name VARCHAR(80) NOT NULL,
  record_id BIGINT NOT NULL,
  action_type VARCHAR(20) NOT NULL,
  snapshot JSON,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_audit_table_record (table_name, record_id)
) COMMENT='历史记录';
