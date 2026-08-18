package com.dinogo.sysmsg.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.dinogo.sysmsg.entity.RecordEntity;
import com.dinogo.sysmsg.entity.RecordStatus;

public interface RecordRepository
        extends JpaRepository<RecordEntity, Integer> {

    // ============================================================
    // 會員收件匣
    // ============================================================

    List<RecordEntity>
    findByMsgtoMemberIdAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoMemberId,
            RecordStatus recordStatus
    );

    List<RecordEntity> findByMsgtoMemberIdAndRecordStatusNotOrderByRecordCreatedAtDesc(Integer id, RecordStatus status);

    Page<RecordEntity>
    findByMsgtoMemberIdAndRecordStatus(
            Integer msgtoMemberId,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 會員收件匣 + msg_function
    // ============================================================

    List<RecordEntity>
    findByMsgtoMemberIdAndMsgFunctionAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoMemberId,
            String msgFunction,
            RecordStatus recordStatus
    );

    Page<RecordEntity>
    findByMsgtoMemberIdAndMsgFunctionStartingWithAndRecordStatus(
            Integer msgtoMemberId,
            String msgFunctionPrefix,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 商家收件匣
    // ============================================================

    List<RecordEntity>
    findByMsgtoSellerIdAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoSellerId,
            RecordStatus recordStatus
    );

    List<RecordEntity> findByMsgtoSellerIdAndRecordStatusNotOrderByRecordCreatedAtDesc(Integer id, RecordStatus status);

    Page<RecordEntity>
    findByMsgtoSellerIdAndRecordStatus(
            Integer msgtoSellerId,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 商家收件匣 + msg_function
    // ============================================================

    List<RecordEntity>
    findByMsgtoSellerIdAndMsgFunctionAndRecordStatusOrderByRecordCreatedAtDesc(
            Integer msgtoSellerId,
            String msgFunction,
            RecordStatus recordStatus
    );

    Page<RecordEntity>
    findByMsgtoSellerIdAndMsgFunctionStartingWithAndRecordStatus(
            Integer msgtoSellerId,
            String msgFunctionPrefix,
            RecordStatus recordStatus,
            Pageable pageable
    );


    // ============================================================
    // 取得某一筆 Record
    // ============================================================

    Optional<RecordEntity> findByRecordId(Integer recordId);


    // ============================================================
    // 權限驗證
    //
    // 會員是否擁有這筆 Record
    // ============================================================

    Optional<RecordEntity>
    findByRecordIdAndMsgtoMemberId(
            Integer recordId,
            Integer msgtoMemberId
    );


    // ============================================================
    // 權限驗證
    //
    // 商家是否擁有這筆 Record
    // ============================================================

    Optional<RecordEntity>
    findByRecordIdAndMsgtoSellerId(
            Integer recordId,
            Integer msgtoSellerId
    );


    // ============================================================
    // 發送紀錄
    //
    // 某個 SEND 建立過哪些 Record
    // ============================================================

    List<RecordEntity>
    findBySend_SendIdOrderByRecordCreatedAtDesc(
            Integer sendId
    );


    // ============================================================
    // 發送紀錄
    //
    // 某個 msg_function 的發送紀錄
    // ============================================================

    List<RecordEntity>
    findByMsgFunctionOrderByRecordCreatedAtDesc(
            String msgFunction
    );


    // ============================================================
    // 發送者紀錄
    // ============================================================

    List<RecordEntity>
    findByMsgfromSellerIdOrderByRecordCreatedAtDesc(
            Integer msgfromSellerId
    );

    boolean existsBySend_SendId(Integer sendId);

    boolean existsByOrderIdAndOrderStatusAndMsgtoMemberId(
            Integer orderId,
            String orderStatus,
            Integer msgtoMemberId
    );

    boolean existsByOrderIdAndOrderStatusAndMsgtoSellerId(
            Integer orderId,
            String orderStatus,
            Integer msgtoSellerId
    );
}
