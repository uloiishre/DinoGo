package com.dinogo.sysmsg.entity;
import org.hibernate.annotations.Nationalized;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/**
 * ============================================================
 * sysmsg.send_seller
 * ============================================================
 *
 * JOINED：
 *
 * SendEntity
 *     ↓
 * SendSellerEntity
 *
 * 用於 SC 商家 → 會員訊息。
 *
 * 也可以存放商家訊息專屬欄位。
 *
 * ============================================================

 */
@Entity
@PrimaryKeyJoinColumn(name = "send_seller_id")
@Table(
    name = "send_seller",
    schema = "sysmsg"
)
public class SendSellerEntity extends SendEntity {


    @Lob
    @Column(
        name = "img_one"
    )
    private byte[] imgOne;

    @Lob
    @Column(
        name = "img_two"
    )
    private byte[] imgTwo;


    @Lob
    @Column(
        name = "img_three"
    )
    private byte[] imgThree;
    
    @Nationalized
    @Column(
        name = "send_remark",
        length = 1000
    )
    private String sendRemark;


    // ============================================================
    // Constructor
    // ============================================================

    /**
     * Lombok： @NoArgsConstructor
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
        String sendRemark
    ) {

        super(
            msgfromSellerId,
            msgFunction,
            msgLabel,
            sendTitle,
            sendContent,
            sendStatus
        );

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
     *     @Getter
     *     @Setter
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
