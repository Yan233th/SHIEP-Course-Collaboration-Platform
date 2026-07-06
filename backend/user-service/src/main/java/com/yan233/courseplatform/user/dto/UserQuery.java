package com.yan233.courseplatform.user.dto;

import com.yan233.courseplatform.common.query.PageQuery;
import lombok.Data;

@Data
public class UserQuery extends PageQuery {
    private String keyword;
    private String roleCode;
    private Integer status;
    private String sortField;
    private String sortOrder;
}

