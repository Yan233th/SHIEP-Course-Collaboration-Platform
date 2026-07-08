package com.yan233.courseplatform.course.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.yan233.courseplatform.common.api.PageResult;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.CourseBrief;
import com.yan233.courseplatform.course.dto.CourseAccess;
import com.yan233.courseplatform.course.dto.CourseQuery;
import com.yan233.courseplatform.course.dto.CourseRequest;
import com.yan233.courseplatform.course.entity.Course;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CourseBizService extends IService<Course> {
    Course create(CourseRequest request, CurrentUser currentUser);

    Course updateCourse(Long id, CourseRequest request, CurrentUser currentUser);

    PageResult<Course> pageCourses(CourseQuery query, CurrentUser currentUser);

    Course cachedDetail(Long id);

    CourseAccess access(Long courseId, CurrentUser currentUser);

    void requireCanView(Long courseId, CurrentUser currentUser);

    void requireCourseStaff(Long courseId, CurrentUser currentUser);

    void requireCanManageMembers(Long courseId, CurrentUser currentUser);

    List<CourseBrief> briefByIds(Collection<Long> ids);

    List<Map<String, Object>> overview();

    List<Map<String, Object>> activityStats(Long courseId);

    List<Map<String, Object>> assignmentSubmissionStats(Long courseId);

    List<Map<String, Object>> auditHistory(int limit);
}
