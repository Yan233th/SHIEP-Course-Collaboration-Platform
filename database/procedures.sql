USE course_collab;
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS sp_course_activity_stats;
DROP PROCEDURE IF EXISTS sp_file_gc_stats;

DELIMITER //
CREATE PROCEDURE sp_course_activity_stats(IN p_course_id BIGINT)
BEGIN
  SELECT 'notice' AS item_type, COUNT(*) AS item_count
  FROM course_notice
  WHERE course_id = p_course_id AND deleted = 0
  UNION ALL
  SELECT 'resource' AS item_type, COUNT(*) AS item_count
  FROM course_resource
  WHERE course_id = p_course_id AND deleted = 0
  UNION ALL
  SELECT 'assignment' AS item_type, COUNT(*) AS item_count
  FROM assignment
  WHERE course_id = p_course_id AND deleted = 0
  UNION ALL
  SELECT 'discussion' AS item_type, COUNT(*) AS item_count
  FROM discussion_post
  WHERE course_id = p_course_id AND deleted = 0
  UNION ALL
  SELECT 'showcase' AS item_type, COUNT(*) AS item_count
  FROM showcase
  WHERE course_id = p_course_id AND deleted = 0;
END //

CREATE PROCEDURE sp_file_gc_stats()
BEGIN
  SELECT
    lifecycle_status,
    COUNT(*) AS file_count,
    COALESCE(SUM(size_bytes), 0) AS total_size_bytes
  FROM v_file_resource_status
  GROUP BY lifecycle_status
  UNION ALL
  SELECT
    CONCAT('QUEUE_STATUS_', status) AS lifecycle_status,
    COUNT(*) AS file_count,
    0 AS total_size_bytes
  FROM file_gc_queue
  WHERE deleted = 0
  GROUP BY status;
END //
DELIMITER ;
