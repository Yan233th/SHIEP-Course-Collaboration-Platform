package com.yan233.courseplatform.course.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.auth.CurrentUser;
import com.yan233.courseplatform.common.dto.FileBrief;
import com.yan233.courseplatform.common.dto.FileOwnerRequest;
import com.yan233.courseplatform.common.dto.FileReferenceReplaceRequest;
import com.yan233.courseplatform.common.dto.FileReferenceRequest;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.client.FileFeignClient;
import com.yan233.courseplatform.course.dto.AssignmentRequest;
import com.yan233.courseplatform.course.dto.AssignmentView;
import com.yan233.courseplatform.course.dto.CourseAccess;
import com.yan233.courseplatform.course.dto.SubmissionRequest;
import com.yan233.courseplatform.course.dto.SubmissionView;
import com.yan233.courseplatform.course.entity.Assignment;
import com.yan233.courseplatform.course.entity.Submission;
import com.yan233.courseplatform.course.mapper.AssignmentMapper;
import com.yan233.courseplatform.course.mapper.SubmissionMapper;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping
public class AssignmentController {
    private static final String ASSIGNMENT_OWNER = "ASSIGNMENT";
    private static final String SUBMISSION_OWNER = "SUBMISSION";

    private final AssignmentMapper assignmentMapper;
    private final SubmissionMapper submissionMapper;
    private final CourseBizService courseService;
    private final FileFeignClient fileFeignClient;

    public AssignmentController(AssignmentMapper assignmentMapper, SubmissionMapper submissionMapper, CourseBizService courseService, FileFeignClient fileFeignClient) {
        this.assignmentMapper = assignmentMapper;
        this.submissionMapper = submissionMapper;
        this.courseService = courseService;
        this.fileFeignClient = fileFeignClient;
    }

    @GetMapping("/assignments")
    public Result<List<AssignmentView>> assignments(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        List<Assignment> assignments = assignmentMapper.selectList(new LambdaQueryWrapper<Assignment>()
                .eq(Assignment::getCourseId, courseId)
                .orderByDesc(Assignment::getCreateTime));
        return Result.ok(toAssignmentViews(assignments));
    }

    @PostMapping("/assignments")
    @Transactional
    public Result<AssignmentView> createAssignment(@RequestBody @Valid AssignmentRequest request, HttpServletRequest servletRequest) {
        courseService.requireCourseStaff(request.getCourseId(), UserContext.from(servletRequest));
        Assignment assignment = new Assignment();
        BeanUtils.copyProperties(request, assignment);
        assignment.setDeleted(0);
        assignmentMapper.insert(assignment);
        bindReference(assignment.getFileId(), ASSIGNMENT_OWNER, assignment.getId());
        return Result.ok(toAssignmentViews(List.of(assignment)).get(0));
    }

    @PutMapping("/assignments/{id}")
    @Transactional
    public Result<AssignmentView> updateAssignment(@PathVariable Long id, @RequestBody @Valid AssignmentRequest request, HttpServletRequest servletRequest) {
        courseService.requireCourseStaff(request.getCourseId(), UserContext.from(servletRequest));
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        Long oldFileId = assignment.getFileId();
        BeanUtils.copyProperties(request, assignment);
        assignment.setId(id);
        assignmentMapper.updateById(assignment);
        replaceReference(oldFileId, assignment.getFileId(), ASSIGNMENT_OWNER, assignment.getId());
        return Result.ok(toAssignmentViews(List.of(assignment)).get(0));
    }

    @DeleteMapping("/assignments/{id}")
    @Transactional
    public Result<Void> deleteAssignment(@PathVariable Long id, HttpServletRequest servletRequest) {
        Assignment assignment = assignmentMapper.selectById(id);
        if (assignment != null) {
            courseService.requireCourseStaff(assignment.getCourseId(), UserContext.from(servletRequest));
            List<Submission> submissions = submissionMapper.selectList(new LambdaQueryWrapper<Submission>()
                    .eq(Submission::getAssignmentId, id));
            releaseOwner(ASSIGNMENT_OWNER, assignment.getId());
            submissions.forEach(submission -> releaseOwner(SUBMISSION_OWNER, submission.getId()));
            submissionMapper.delete(new LambdaQueryWrapper<Submission>().eq(Submission::getAssignmentId, id));
        }
        assignmentMapper.deleteById(id);
        return Result.ok();
    }

    @PostMapping("/submissions")
    @Transactional
    public Result<SubmissionView> submit(@RequestBody @Valid SubmissionRequest request, HttpServletRequest servletRequest) {
        CurrentUser current = UserContext.from(servletRequest);
        Assignment assignment = requireAssignment(request.getAssignmentId());
        CourseAccess access = courseService.access(assignment.getCourseId(), current);
        if (!access.actions().contains("SUBMIT_ASSIGNMENT")) {
            throw new BusinessException(403, "无权限提交该课程作业");
        }
        if (request.getFileId() == null && (request.getContent() == null || request.getContent().isBlank())) {
            throw new BusinessException(400, "请填写提交内容或上传附件");
        }
        if (!current.isAdmin() && assignment.getDueTime() != null && LocalDateTime.now().isAfter(assignment.getDueTime())) {
            throw new BusinessException(400, "作业已截止，不能重新提交");
        }

        Long studentId = current.isAdmin() ? request.getStudentId() : current.userId();
        Submission existing = submissionMapper.selectOne(new LambdaQueryWrapper<Submission>()
                .eq(Submission::getAssignmentId, assignment.getId())
                .eq(Submission::getStudentId, studentId)
                .last("LIMIT 1"));
        if (existing != null) {
            Long oldFileId = existing.getFileId();
            existing.setFileId(request.getFileId());
            existing.setContent(request.getContent());
            existing.setScore(null);
            existing.setFeedback(null);
            existing.setStatus(0);
            submissionMapper.updateById(existing);
            replaceReference(oldFileId, existing.getFileId(), SUBMISSION_OWNER, existing.getId());
            return Result.ok(toSubmissionViews(List.of(existing)).get(0));
        }

        Submission submission = new Submission();
        BeanUtils.copyProperties(request, submission);
        submission.setStudentId(studentId);
        submission.setStatus(0);
        submission.setDeleted(0);
        submissionMapper.insert(submission);
        bindReference(submission.getFileId(), SUBMISSION_OWNER, submission.getId());
        return Result.ok(toSubmissionViews(List.of(submission)).get(0));
    }

    @PutMapping("/submissions/{id}/grade")
    public Result<SubmissionView> grade(@PathVariable Long id, @RequestBody @Valid SubmissionRequest request, HttpServletRequest servletRequest) {
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
        return Result.ok(toSubmissionViews(List.of(submission)).get(0));
    }

    @GetMapping("/submissions")
    public Result<List<SubmissionView>> submissions(@RequestParam(required = false) Long assignmentId,
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
        return Result.ok(toSubmissionViews(submissionMapper.selectList(wrapper)));
    }

    private Assignment requireAssignment(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException("作业不存在");
        }
        return assignment;
    }

    private List<AssignmentView> toAssignmentViews(List<Assignment> assignments) {
        Map<Long, FileBrief> files = filesById(assignments.stream().map(Assignment::getFileId).toList());
        return assignments.stream()
                .map(assignment -> new AssignmentView(
                        assignment.getId(),
                        assignment.getCourseId(),
                        assignment.getTitle(),
                        assignment.getDescription(),
                        assignment.getFileId(),
                        files.get(assignment.getFileId()),
                        assignment.getDueTime(),
                        assignment.getTotalScore(),
                        assignment.getStatus(),
                        assignment.getCreateTime(),
                        assignment.getUpdateTime()))
                .toList();
    }

    private List<SubmissionView> toSubmissionViews(List<Submission> submissions) {
        Map<Long, FileBrief> files = filesById(submissions.stream().map(Submission::getFileId).toList());
        return submissions.stream()
                .map(submission -> new SubmissionView(
                        submission.getId(),
                        submission.getAssignmentId(),
                        submission.getStudentId(),
                        submission.getFileId(),
                        files.get(submission.getFileId()),
                        submission.getContent(),
                        submission.getScore(),
                        submission.getFeedback(),
                        submission.getStatus(),
                        submission.getCreateTime(),
                        submission.getUpdateTime()))
                .toList();
    }

    private Map<Long, FileBrief> filesById(List<Long> fileIds) {
        List<Long> ids = fileIds.stream()
                .flatMap(id -> id == null ? Stream.empty() : Stream.of(id))
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Result<List<FileBrief>> result = fileFeignClient.filesByIds(ids);
            if (result.getCode() == 200 && result.getData() != null) {
                return result.getData().stream().collect(Collectors.toMap(FileBrief::id, Function.identity(), (a, b) -> a));
            }
        } catch (RuntimeException ignored) {
            return Collections.emptyMap();
        }
        return Collections.emptyMap();
    }

    private void bindReference(Long fileId, String ownerType, Long ownerId) {
        if (fileId == null) {
            return;
        }
        FileReferenceRequest request = new FileReferenceRequest();
        request.setFileId(fileId);
        request.setOwnerType(ownerType);
        request.setOwnerId(ownerId);
        fileFeignClient.bindReference(request);
    }

    private void replaceReference(Long oldFileId, Long newFileId, String ownerType, Long ownerId) {
        FileReferenceReplaceRequest request = new FileReferenceReplaceRequest();
        request.setOldFileId(oldFileId);
        request.setNewFileId(newFileId);
        request.setOwnerType(ownerType);
        request.setOwnerId(ownerId);
        fileFeignClient.replaceReference(request);
    }

    private void releaseOwner(String ownerType, Long ownerId) {
        FileOwnerRequest request = new FileOwnerRequest();
        request.setOwnerType(ownerType);
        request.setOwnerId(ownerId);
        fileFeignClient.releaseOwner(request);
    }
}
