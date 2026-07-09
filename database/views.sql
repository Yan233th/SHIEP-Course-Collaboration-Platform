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

CREATE OR REPLACE VIEW v_file_resource_status AS
SELECT
  fm.id AS file_id,
  fm.original_name,
  fm.content_hash,
  COALESCE(
    GROUP_CONCAT(
      DISTINCT CASE WHEN fr.deleted = 0 AND fr.status = 1 THEN fr.owner_type END
      ORDER BY fr.owner_type
      SEPARATOR ','
    ),
    fm.biz_type
  ) AS biz_type,
  fm.size_bytes,
  fm.uploader_id,
  fm.deleted AS file_deleted,
  COUNT(DISTINCT CASE WHEN fr.deleted = 0 AND fr.status = 1 THEN fr.id END) AS active_reference_count,
  COUNT(DISTINCT CASE WHEN q.deleted = 0 AND q.status = 0 THEN q.id END) AS pending_gc_count,
  MAX(q.processed_time) AS last_processed_time,
  CASE
    WHEN fm.deleted = 1 THEN 'DELETED'
    WHEN COUNT(DISTINCT CASE WHEN fr.deleted = 0 AND fr.status = 1 THEN fr.id END) > 0 THEN 'IN_USE'
    WHEN COUNT(DISTINCT CASE WHEN q.deleted = 0 AND q.status = 0 THEN q.id END) > 0 THEN 'PENDING_GC'
    ELSE 'ORPHAN'
  END AS lifecycle_status
FROM file_metadata fm
LEFT JOIN file_reference fr ON fr.file_id = fm.id
LEFT JOIN file_gc_queue q ON q.file_id = fm.id
GROUP BY fm.id, fm.original_name, fm.content_hash, fm.biz_type, fm.size_bytes, fm.uploader_id, fm.deleted;
