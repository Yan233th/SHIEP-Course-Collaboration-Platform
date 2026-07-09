USE course_collab;
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_resource_update_history;
DROP TRIGGER IF EXISTS trg_submission_grade_history;
DROP TRIGGER IF EXISTS trg_submission_resubmit_history;
DROP TRIGGER IF EXISTS trg_project_member_insert_history;
DROP TRIGGER IF EXISTS trg_file_reference_release_queue;

DELIMITER //
CREATE TRIGGER trg_resource_update_history
AFTER UPDATE ON course_resource
FOR EACH ROW
BEGIN
  INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
  VALUES (
    'course_resource',
    NEW.id,
    IF(NEW.deleted = 1 AND OLD.deleted = 0, 'DELETE', 'UPDATE'),
    JSON_OBJECT('old_title', OLD.title, 'new_title', NEW.title, 'category', NEW.category, 'tags', NEW.tags)
  );
END //

CREATE TRIGGER trg_submission_grade_history
AFTER UPDATE ON assignment_submission
FOR EACH ROW
BEGIN
  IF NEW.status = 1 AND (NOT (OLD.score <=> NEW.score) OR NOT (OLD.feedback <=> NEW.feedback)) THEN
    INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
    VALUES (
      'assignment_submission',
      NEW.id,
      'GRADE',
      JSON_OBJECT('student_id', NEW.student_id, 'score', NEW.score, 'feedback', NEW.feedback)
    );
  END IF;
END //

CREATE TRIGGER trg_submission_resubmit_history
AFTER UPDATE ON assignment_submission
FOR EACH ROW
BEGIN
  IF OLD.deleted = 0
    AND NEW.deleted = 0
    AND NEW.status = 0
    AND (NOT (OLD.content <=> NEW.content) OR NOT (OLD.file_id <=> NEW.file_id)) THEN
    INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
    VALUES (
      'assignment_submission',
      NEW.id,
      'RESUBMIT',
      JSON_OBJECT('assignment_id', NEW.assignment_id, 'student_id', NEW.student_id, 'file_id', NEW.file_id)
    );
  END IF;
END //

CREATE TRIGGER trg_project_member_insert_history
AFTER INSERT ON project_member
FOR EACH ROW
BEGIN
  INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
  VALUES (
    'project_member',
    NEW.id,
    'JOIN',
    JSON_OBJECT('group_id', NEW.group_id, 'user_id', NEW.user_id, 'role_name', NEW.role_name)
  );
END //

CREATE TRIGGER trg_file_reference_release_queue
AFTER UPDATE ON file_reference
FOR EACH ROW
BEGIN
  IF OLD.deleted = 0 AND NEW.deleted = 1 THEN
    INSERT INTO file_gc_queue(file_id, source_reference_id, reason, status, next_retry_time)
    VALUES (OLD.file_id, OLD.id, 'REFERENCE_RELEASED', 0, NOW());

    INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
    VALUES (
      'file_reference',
      NEW.id,
      'RELEASE',
      JSON_OBJECT('file_id', OLD.file_id, 'owner_type', OLD.owner_type, 'owner_id', OLD.owner_id)
    );
  END IF;
END //
DELIMITER ;
