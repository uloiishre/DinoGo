package com.dinogo.msg.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * ============================================================
 * sysmsg.send_seller
 * ============================================================
 *
 * JOINED：
 *
 * SendEntity
 * ↓
 * SendSellerEntity
 *
 * 用於 SC 商家 → 會員訊息。
 *
 * 也可以存放商家訊息專屬欄位。
 *
 * ============================================================
 *
 * img_one / img_two / img_three
 *
 * SQL Server：
 *
 * VARBINARY(MAX)
 *
 * 這裡直接映射，不另外處理 img%。
 */
@Entity
@Table(name = "send_seller", schema = "sysmsg")
public class SendSellerEntity extends SendEntity {

    /**
     * ------------------------------------------------------------
     * img_one
     * ------------------------------------------------------------
     *
     * SQL Server：
     * VARBINARY(MAX)
     */
    @Lob
    @Column(name = "img_one")
    private byte[] imgOne;

    /**
     * ------------------------------------------------------------
     * img_two
     * ------------------------------------------------------------
     */
    @Lob
    @Column(name = "img_two")
    private byte[] imgTwo;

    /**
     * ------------------------------------------------------------
     * img_three
     * ------------------------------------------------------------
     */
    @Lob
    @Column(name = "img_three")
    private byte[] imgThree;

    /**
     * ------------------------------------------------------------
     * send_remark
     * ------------------------------------------------------------
     */
    @Column(name = "send_remark", length = 1000)
    private String sendRemark;

    // ============================================================
    // Constructor
    // ============================================================

    /**
     * Lombok：
     *
     * @NoArgsConstructor
     */
    protected SendSellerEntity() {
        super();
    }

    public SendSellerEntity(
            Integer msgfromSellerId,
            String msgFunction,
            String msgLabel,
            String sendTitle,
            String sendContent,
            SendStatus sendStatus,
            byte[] imgOne,
            byte[] imgTwo,
            byte[] imgThree,
            String sendRemark) {

        super(
                msgfromSellerId,
                msgFunction,
                msgLabel,
                sendTitle,
                sendContent,
                sendStatus);

        this.imgOne = imgOne;
        this.imgTwo = imgTwo;
        this.imgThree = imgThree;
        this.sendRemark = sendRemark;
    }

    // ============================================================
    // Getter / Setter
    // ============================================================

    /*
     * Lombok：
     *
     * @Getter
     * 
     * @Setter
     */

    public byte[] getImgOne() {
        return imgOne;
    }

    public void setImgOne(byte[] imgOne) {
        this.imgOne = imgOne;
    }

    public byte[] getImgTwo() {
        return imgTwo;
    }

    public void setImgTwo(byte[] imgTwo) {
        this.imgTwo = imgTwo;
    }

    public byte[] getImgThree() {
        return imgThree;
    }

    public void setImgThree(byte[] imgThree) {
        this.imgThree = imgThree;
    }

    public String getSendRemark() {
        return sendRemark;
    }

    public void setSendRemark(String sendRemark) {
        this.sendRemark = sendRemark;
    }
}