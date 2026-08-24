package com.dinogo.sysmsg.dto.request.send;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 商家直接建立 SC 實際訊息。
 *
 * 不建立 SAVE。
 *
 * 直接：
 *
 * SendEntity
 *     +
 * SendSellerEntity
 *
 * send_status = SEND
 *
 * 必須驗證 Order。
 */
public class SellerCreateRequest {

    /**
     * 商家選擇的訂單。
     */
    @NotNull
    private Integer orderId;

    @NotBlank
    @Size(max = 100)
    private String sendTitle;

    @NotBlank
    @Size(max = 1000)
    private String sendContent;

    @Size(max = 1000)
    private String sendRemark;

    private byte[] imgOne;

    private byte[] imgTwo;

    private byte[] imgThree;

    public SellerCreateRequest() {
    }

    public Integer getOrderId() {
        return orderId;
    }

    public void setOrderId(Integer orderId) {
        this.orderId = orderId;
    }

    public String getSendTitle() {
        return sendTitle;
    }

    public void setSendTitle(String sendTitle) {
        this.sendTitle = sendTitle;
    }

    public String getSendContent() {
        return sendContent;
    }

    public void setSendContent(String sendContent) {
        this.sendContent = sendContent;
    }

    public String getSendRemark() {
        return sendRemark;
    }

    public void setSendRemark(String sendRemark) {
        this.sendRemark = sendRemark;
    }

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
}
