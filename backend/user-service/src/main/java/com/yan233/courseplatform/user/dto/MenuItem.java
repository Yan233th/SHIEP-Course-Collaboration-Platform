package com.yan233.courseplatform.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class MenuItem {
    private String title;
    private String path;
    private List<MenuItem> children;
}

