package com.vetautet.infrastructure.persistence.repository;

import com.vetautet.infrastructure.persistence.entity.OutboxEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {
    @Query("""
            select event from OutboxEventEntity event
            where event.published = false
              and (event.nextRetryAt is null or event.nextRetryAt <= :now)
            order by event.createdAt asc, event.id asc
            """)
    List<OutboxEventEntity> findPendingForPublish(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("""
            select event.id from OutboxEventEntity event
            where event.published = true
              and event.publishedAt < :before
            order by event.publishedAt asc, event.id asc
            """)
    List<Long> findPublishedIdsBefore(@Param("before") LocalDateTime before, Pageable pageable);

    @Modifying
    @Query("delete from OutboxEventEntity event where event.id in :ids")
    int deleteByIds(@Param("ids") List<Long> ids);
}
