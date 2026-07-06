package com.yan233.courseplatform.course.controller;

import com.yan233.courseplatform.common.api.Result;
import com.yan233.courseplatform.common.auth.AccessControl;
import com.yan233.courseplatform.common.web.UserContext;
import com.yan233.courseplatform.course.service.CourseBizService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/stats")
public class StatsController {
    private final CourseBizService courseService;

    public StatsController(CourseBizService courseService) {
        this.courseService = courseService;
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
}
