package com.dinogo.sysmsg.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dinogo.sysmsg.entity.SendEntity;
import com.dinogo.sysmsg.entity.SendStatus;

/** sysmsg.send 的一般查詢；流水號鎖定由 MsgFunctionSequenceRepository 負責。 */
public interface SendRepository extends JpaRepository<SendEntity, Integer> {

    List<SendEntity> findByMsgfromSellerIdAndSendStatusOrderBySendUpdAtDesc(
            Integer msgfromSellerId, SendStatus sendStatus);

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
