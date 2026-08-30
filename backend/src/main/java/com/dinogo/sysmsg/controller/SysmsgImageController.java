package com.dinogo.sysmsg.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.dinogo.security.AuthenticatedMember;
import com.dinogo.sysmsg.dto.response.SysmsgImageUploadResponse;
import com.dinogo.sysmsg.service.SysmsgImageService;

//sysmsg-start，總共1次修改，第1次//
/** 可獨立整併的 Sysmsg 圖片 API，不依賴尚未整併的 SendService。 */
@RestController
@RequestMapping("/api/sysmsg/seller/messages/images")
public class SysmsgImageController {
    private final SysmsgImageService images;
    public SysmsgImageController(SysmsgImageService images) { this.images = images; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SysmsgImageUploadResponse upload(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedMember member) {
        if (member == null || member.memberId() == null) throw new AuthenticationCredentialsNotFoundException("需要登入會員身分");
        return new SysmsgImageUploadResponse(images.upload(files, member.memberId()));
    }
}
//sysmsg-end，總共1次修改，第1次//
