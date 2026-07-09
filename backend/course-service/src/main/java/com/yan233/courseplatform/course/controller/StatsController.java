package com.yan233.courseplatform.course.controller;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.exception.BusinessException;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.client.FileFeignClient;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final CourseBizService courseService;
    private final FileFeignClient fileFeignClient;

    public StatsController(CourseBizService courseService, FileFeignClient fileFeignClient) {
        this.courseService = courseService;
        this.fileFeignClient = fileFeignClient;
    }

    @GetMapping("/course-overview")
    public Result<List<Map<String, Object>>> overview(HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        return Result.ok(courseService.overview());
    }

    @GetMapping("/course-activity")
    public Result<List<Map<String, Object>>> activity(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        return Result.ok(courseService.activityStats(courseId));
    }

    @GetMapping("/assignment-submissions")
    public Result<List<Map<String, Object>>> assignmentSubmissions(@RequestParam Long courseId, HttpServletRequest servletRequest) {
        courseService.requireCanView(courseId, UserContext.from(servletRequest));
        return Result.ok(courseService.assignmentSubmissionStats(courseId));
    }

    @GetMapping("/audit-history")
    public Result<List<Map<String, Object>>> auditHistory(@RequestParam(defaultValue = "30") int limit,
                                                          @RequestParam(required = false) String tableName,
                                                          @RequestParam(required = false) String actionType,
                                                          @RequestParam(required = false) Long recordId,
                                                          HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        return Result.ok(courseService.auditHistory(tableName, actionType, recordId, limit));
    }

    @GetMapping("/file-status")
    public Result<List<Map<String, Object>>> fileStatus(HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        return unwrap(fileFeignClient.fileStatuses());
    }

    @GetMapping("/file-gc")
    public Result<List<Map<String, Object>>> fileGc(HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        return unwrap(fileFeignClient.gcStats());
    }

    @PostMapping("/file-gc/run")
    public Result<Integer> runFileGc(HttpServletRequest servletRequest) {
        AccessControl.requireRole(UserContext.from(servletRequest), "ADMIN");
        Result<Integer> result = fileFeignClient.runGc();
        if (result.getCode() != 200) {
            throw new BusinessException(result.getCode(), result.getMessage());
        }
        return Result.ok(result.getData());
    }

    private Result<List<Map<String, Object>>> unwrap(Result<List<Map<String, Object>>> result) {
        if (result.getCode() != 200) {
            throw new BusinessException(result.getCode(), result.getMessage());
        }
        return Result.ok(result.getData());
    }
}
