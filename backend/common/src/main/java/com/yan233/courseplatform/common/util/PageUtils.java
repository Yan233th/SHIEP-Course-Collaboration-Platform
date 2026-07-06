package com.yan233.courseplatform.common.util;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yan233.courseplatform.common.api.PageResult;

public final class PageUtils {
    private PageUtils() {
    }

    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
    }
}

