USE course_collab;
SET NAMES utf8mb4;

CREATE OR REPLACE VIEW v_course_overview AS
SELECT
  c.id AS course_id,
  c.course_code,
  c.course_name,
  c.teacher_name,
  COUNT(DISTINCT cm.id) AS member_count,
  COUNT(DISTINCT cr.id) AS resource_count,
  COUNT(DISTINCT a.id) AS assignment_count,
  COUNT(DISTINCT pg.id) AS project_group_count
FROM course c
LEFT JOIN course_member cm ON cm.course_id = c.id AND cm.deleted = 0
LEFT JOIN course_resource cr ON cr.course_id = c.id AND cr.deleted = 0
LEFT JOIN assignment a ON a.course_id = c.id AND a.deleted = 0
LEFT JOIN project_group pg ON pg.course_id = c.id AND pg.deleted = 0
WHERE c.deleted = 0
GROUP BY c.id, c.course_code, c.course_name, c.teacher_name;

CREATE OR REPLACE VIEW v_assignment_submission_stats AS
SELECT
  a.id AS assignment_id,
  a.course_id,
  a.title,
  COUNT(s.id) AS submitted_count,
  SUM(CASE WHEN s.status = 1 THEN 1 ELSE 0 END) AS graded_count,
  AVG(s.score) AS avg_score
FROM assignment a
LEFT JOIN assignment_submission s ON s.assignment_id = a.id AND s.deleted = 0
WHERE a.deleted = 0
GROUP BY a.id, a.course_id, a.title;
