package com.dinogo.sysmsg.dto.request.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商家後台建立 SC 範本。
 *
 * seller_id 不由前端傳入。
 *
 * 由：
 * 登入會員
 *   ↓
 * MemberAuthService
 *   ↓
 * SellerInfo
 *   ↓
 * seller_id
 *
 * 自動取得。
 */
public class SellerTemplateCreateRequest {

    @Size(max = 50)
    private String msgLabel;

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

    /*
     * Lombok 可使用：
     *
     * @Getter
     * @Setter
     * @NoArgsConstructor
     */

    public SellerTemplateCreateRequest() {
    }

    public String getMsgLabel() {
        return msgLabel;
    }

    public void setMsgLabel(String msgLabel) {
        this.msgLabel = msgLabel;
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
