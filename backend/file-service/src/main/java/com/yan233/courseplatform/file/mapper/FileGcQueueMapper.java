package com.yan233.courseplatform.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yan233.courseplatform.file.entity.FileGcQueue;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

@Mapper
public interface FileGcQueueMapper extends BaseMapper<FileGcQueue> {
    @Select("""
            SELECT *
            FROM file_gc_queue
            WHERE deleted = 0
              AND status IN (0, 4)
              AND attempts < #{maxAttempts}
              AND next_retry_time <= NOW()
            ORDER BY create_time ASC
            LIMIT #{limit}
            """)
    List<FileGcQueue> selectRunnable(@Param("limit") int limit, @Param("maxAttempts") int maxAttempts);

    @Select("""
            SELECT *
            FROM file_gc_queue
            WHERE deleted = 0
              AND status IN (0, 4)
              AND attempts < #{maxAttempts}
            ORDER BY create_time ASC
            LIMIT #{limit}
            """)
    List<FileGcQueue> selectRunnableNow(@Param("limit") int limit, @Param("maxAttempts") int maxAttempts);

    @Update("""
            UPDATE file_gc_queue
            SET status = 1,
                attempts = attempts + 1,
                last_error = NULL
            WHERE id = #{id}
              AND deleted = 0
              AND status IN (0, 4)
            """)
    int markProcessing(@Param("id") Long id);

    @Insert("""
            INSERT INTO file_gc_queue(file_id, reason, status, next_retry_time)
            SELECT fm.id, 'ORPHAN_SWEEP', 0, NOW()
            FROM file_metadata fm
            WHERE fm.deleted = 0
              AND fm.create_time < TIMESTAMPADD(MINUTE, -#{olderThanMinutes}, NOW())
              AND NOT EXISTS (
                SELECT 1
                FROM file_reference fr
                WHERE fr.file_id = fm.id
                  AND fr.deleted = 0
                  AND fr.status = 1
              )
              AND NOT EXISTS (
                SELECT 1
                FROM file_gc_queue q
                WHERE q.file_id = fm.id
                  AND q.deleted = 0
                  AND q.status IN (0, 1, 2, 4)
              )
            """)
    int enqueueOrphans(@Param("olderThanMinutes") int olderThanMinutes);

    @Select("CALL sp_file_gc_stats()")
    List<Map<String, Object>> gcStats();

    @Select("SELECT * FROM v_file_resource_status ORDER BY file_deleted, active_reference_count DESC, file_id DESC")
    List<Map<String, Object>> fileStatuses();
}
