USE course_collab;
SET NAMES utf8mb4;

DROP TRIGGER IF EXISTS trg_resource_update_history;
DROP TRIGGER IF EXISTS trg_submission_grade_history;
DROP TRIGGER IF EXISTS trg_project_member_insert_history;

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
  IF OLD.score IS NULL OR OLD.score <> NEW.score OR OLD.feedback <> NEW.feedback THEN
    INSERT INTO audit_history(table_name, record_id, action_type, snapshot)
    VALUES (
      'assignment_submission',
      NEW.id,
      'GRADE',
      JSON_OBJECT('student_id', NEW.student_id, 'score', NEW.score, 'feedback', NEW.feedback)
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
DELIMITER ;
