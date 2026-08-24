package com.dinogo.sysmsg.repository;

import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;

/** sysmsg.send 的一般查詢；流水號鎖定由 MsgFunctionSequenceRepository 負責。 */
public interface SendRepository extends JpaRepository<SendEntity, Integer> {

    @Query("""
            select s from SendEntity s
            where s.msgfromSellerId = :senderId and s.sendStatus = :status
              and (:prefix is null or s.msgFunction like concat(:prefix, '%'))
              and (:cursorTime is null or s.sendUpdAt < :cursorTime
                   or (s.sendUpdAt = :cursorTime and s.sendId < :cursorId))
            order by s.sendUpdAt desc, s.sendId desc
            """)
    List<SendEntity> findBySenderAndStatusNewest(
            @Param("senderId") Integer senderId, @Param("status") SendStatus status,
            @Param("prefix") String prefix,
            @Param("cursorTime") LocalDateTime cursorTime, @Param("cursorId") Integer cursorId,
            Pageable pageable);

    @Query("""
            select s from SendEntity s
            where s.msgfromSellerId = :senderId and s.sendStatus = :status
              and (:prefix is null or s.msgFunction like concat(:prefix, '%'))
              and (:cursorTime is null or s.sendUpdAt > :cursorTime
                   or (s.sendUpdAt = :cursorTime and s.sendId > :cursorId))
            order by s.sendUpdAt asc, s.sendId asc
            """)
    List<SendEntity> findBySenderAndStatusOldest(
            @Param("senderId") Integer senderId, @Param("status") SendStatus status,
            @Param("prefix") String prefix,
            @Param("cursorTime") LocalDateTime cursorTime, @Param("cursorId") Integer cursorId,
            Pageable pageable);

    List<SendEntity> findByMsgFunctionAndSendStatus(
            String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgfromSellerIdAndMsgFunctionAndSendStatus(
            Integer msgfromSellerId, String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgFunctionOrderBySendUpdAtDesc(String msgFunction);

    List<SendEntity> findByMsgFunctionAndSendStatusOrderBySendUpdAtDesc(
            String msgFunction, SendStatus sendStatus);

    List<SendEntity> findByMsgfromSellerIdAndMsgFunctionOrderBySendUpdAtDesc(
            Integer msgfromSellerId, String msgFunction);

    List<SendEntity> findBySendStatusOrderBySendUpdAtDesc(SendStatus sendStatus);

    boolean existsByMsgFunctionAndSendStatus(
            String msgFunction, SendStatus sendStatus);
}
