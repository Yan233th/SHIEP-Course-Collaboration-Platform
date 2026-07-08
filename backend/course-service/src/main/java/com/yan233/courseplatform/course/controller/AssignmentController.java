package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.dto.CourseAccess;
import com.yan233.courseplatform.course.dto.AssignmentRequest;
import com.yan233.courseplatform.course.dto.SubmissionRequest;
import com.yan233.courseplatform.course.entity.Assignment;
import com.yan233.courseplatform.course.entity.Submission;
import com.yan233.courseplatform.course.mapper.AssignmentMapper;
import com.yan233.courseplatform.course.mapper.SubmissionMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class AssignmentController {
    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseBizService courseService;

    public AssignmentController(AssignmentMapper assignmentMapper, SubmissionMapper submissionMapper, CourseBizService courseService) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.courseService = courseService;
    }

    @GetMapping("/assignments")
    public Result<List<Assignment>> assignments(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        return Result.ok(assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getCourseId, courseId)
                .orderByDesc(Assignment::getCreateTime)));
    }

    @PostMapping("/assignments")
    public Result<Assignment> createAssignment(@RequestBody @Valid AssignmentRequest request, HttpServletRequest servletRequest) {
        courseService.requireCourseStaff(request.getCourseId(), UserContext.from(servletRequest));
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(request, assignment);
        assignment.setDeleted(0);
        assignmentMapper.insert(assignment);
        return Result.ok(assignment);
    }

    @PutMapping("/assignments/{id}")
    public Result<Assignment> updateAssignment(@PathVariable Long id, @RequestBody @Valid AssignmentRequest request, HttpServletRequest servletRequest) {
        courseService.requireCourseStaff(request.getCourseId(), UserContext.from(servletRequest));
        Assignment assignment = assignmentMapper.selectById(id);
        BeanUtils.copyProperties(request, assignment);
        assignment.setId(id);
        assignmentMapper.updateById(assignment);
        return Result.ok(assignment);
    }

    @PostMapping("/submissions")
    public Result<Submission> submit(@RequestBody @Valid SubmissionRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        Assignment assignment = requireAssignment(request.getAssignmentId());
        CourseAccess access = courseService.access(assignment.getCourseId(), current);
        if (!access.actions().contains("SUBMIT_ASSIGNMENT")) {
            throw new BusinessException(403, "无权限提交该课程作业");
        }
        if (!current.isAdmin()) {
            request.setStudentId(current.userId());
        }
        Submission submission = new Submission();
        BeanUtils.copyProperties(request, submission);
        submission.setDeleted(0);
        submissionMapper.insert(submission);
        return Result.ok(submission);
    }

    @PutMapping("/submissions/{id}/grade")
    public Result<Submission> grade(@PathVariable Long id, @RequestBody @Valid SubmissionRequest request, HttpServletRequest servletRequest) {
        Submission submission = submissionMapper.selectById(id);
        if (submission == null) {
            throw new BusinessException("提交记录不存在");
        }
        Assignment assignment = requireAssignment(submission.getAssignmentId());
        courseService.requireCourseStaff(assignment.getCourseId(), UserContext.from(servletRequest));
        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());
        submission.setStatus(1);
        submissionMapper.updateById(submission);
        return Result.ok(submission);
    }

    @GetMapping("/submissions")
    public Result<List<Submission>> submissions(@RequestParam(required = false) Long assignmentId,
                                                @RequestParam(required = false) Long studentId,
                                                HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        if (assignmentId != null) {
            Assignment assignment = requireAssignment(assignmentId);
            courseService.requireCanView(assignment.getCourseId(), current);
            if (current.isStudent()) {
                studentId = current.userId();
            }
        } else if (current.isStudent()) {
            studentId = current.userId();
        } else {
            AccessControl.requireRole(current, "ADMIN");
        }
        LambdaQueryWrapper<Submission> wrapper = new LambdaQueryWrapper<Submission>()
                .eq(assignmentId != null, Submission::getAssignmentId, assignmentId)
                .eq(studentId != null, Submission::getStudentId, studentId)
                .orderByDesc(Submission::getCreateTime);
        return Result.ok(submissionMapper.selectList(wrapper));
    }

    private Assignment requireAssignment(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        return assignment;
    }
}
