USE course_collab;
SET NAMES utf8mb4;

SET @pwd = '$2b$10$METdqHWkKlYfVDLzj6Z61uL7vXwX.vQiVRVWXwTVKVHAaz9Ldj72S';

INSERT INTO sys_user (id, username, password, real_name, role_code, email, phone, gender, status) VALUES
  (1, 'admin', @pwd, '系统管理员', 'ADMIN', 'admin@example.com', '13800000001', 'U', 1),
  (2, 'teacher1', @pwd, '李老师', 'TEACHER', 'teacher1@example.com', '13800000002', 'F', 1),
  (3, 'ta1', @pwd, '课程助教', 'TA', 'ta1@example.com', '13800000003', 'M', 1),
  (4, 'student1', @pwd, '张三', 'STUDENT', 'student1@example.com', '13800000004', 'M', 1),
  (5, 'student2', @pwd, '李四', 'STUDENT', 'student2@example.com', '13800000005', 'F', 1);

INSERT INTO course (id, course_code, course_name, teacher_id, teacher_name, description, credit, hours, max_students, current_students, status) VALUES
  (1, 'CS-JAVAEE-2026', 'JavaEE 企业应用开发', 2, '李老师', '围绕 Spring Boot、Spring Cloud 与前后端分离应用开发。', 3.0, 48, 80, 2, 1),
  (2, 'CS-DB-2026', '数据库应用课程设计', 2, '李老师', '围绕 MySQL 数据建模、SQL 对象和应用集成。', 2.0, 32, 60, 2, 1),
  (3, 'CS-SE-2026', '软件工程实践', 2, '李老师', '需求分析、项目协作和成果展示。', 2.5, 40, 60, 0, 1);

INSERT INTO course_member (course_id, user_id, role_code) VALUES
  (1, 2, 'TEACHER'),
  (1, 3, 'TA'),
  (1, 4, 'STUDENT'),
  (1, 5, 'STUDENT'),
  (2, 2, 'TEACHER'),
  (2, 4, 'STUDENT');

INSERT INTO file_metadata (id, original_name, storage_name, storage_path, content_type, size_bytes, uploader_id, biz_type) VALUES
  (1, 'JavaEE课程说明.pdf', 'seed-javaee.pdf', 'database/seed-files/seed-javaee.pdf', 'application/pdf', 743, 2, 'resource');

INSERT INTO course_notice (course_id, title, content, publisher_id, pinned, status) VALUES
  (1, '第一次课程项目分组通知', '请同学们在本周内完成课程项目组队。', 2, 1, 1),
  (1, '作业一发布', '请提交需求分析和数据库概念设计。', 3, 0, 1);

INSERT INTO course_resource (course_id, file_id, title, category, tags, description, uploader_id, status) VALUES
  (1, 1, '课程设计要求', '课件', 'JavaEE,数据库,课设', '课程设计完整要求说明。', 2, 1);

INSERT INTO assignment (id, course_id, title, description, due_time, total_score, status) VALUES
  (1, 1, '需求分析与ER图', '提交需求分析、ER图和关系模式。', '2026-07-10 23:59:59', 100, 1),
  (2, 1, '后端接口实现', '提交后端接口和测试截图。', '2026-07-17 23:59:59', 100, 1);

INSERT INTO assignment_submission (assignment_id, student_id, file_id, content, score, feedback, status) VALUES
  (1, 4, NULL, '已完成需求分析和初版ER图。', 88, '结构清晰，补充约束说明。', 1),
  (1, 5, NULL, '提交项目背景和实体说明。', NULL, NULL, 0);

INSERT INTO project_group (id, course_id, name, topic, leader_id, max_members, current_members, status) VALUES
  (1, 1, '协同平台A组', '课程协同管理平台', 4, 5, 2, 1);

INSERT INTO project_member (group_id, user_id, role_name, status) VALUES
  (1, 4, '组长', 1),
  (1, 5, '成员', 1);

INSERT INTO discussion_post (course_id, group_id, parent_id, author_id, title, content, status) VALUES
  (1, 1, NULL, 4, '资源标签怎么设计', '建议资源支持分类和标签组合查询。', 1),
  (1, 1, 1, 3, '回复：资源标签怎么设计', '可以在表中保存标签字符串，报告中说明后续可规范化。', 1);

INSERT INTO showcase (course_id, group_id, file_id, title, summary, link_url, status) VALUES
  (1, 1, NULL, '课程协同管理平台原型', '展示课程、作业、项目和讨论统一管理。', 'https://example.com/demo', 1);
