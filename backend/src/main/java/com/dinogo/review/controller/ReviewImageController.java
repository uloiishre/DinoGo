package com.dinogo.review.controller;

import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.dinogo.review.dto.response.ReviewImageUploadResponse;
import com.dinogo.review.service.ReviewImageService;
import com.dinogo.security.AuthenticatedMember;

//review-start，總共1次修改，第1次//
/** 可獨立整併的 Review 圖片 API，不依賴尚未整併的 ReviewService。 */
@RestController
@RequestMapping("/api/reviews/stars/images")
public class ReviewImageController {
    private final ReviewImageService images;
    public ReviewImageController(ReviewImageService images) { this.images = images; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewImageUploadResponse> upload(
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedMember member) {
        if (member == null || member.memberId() == null) throw new AuthenticationCredentialsNotFoundException("需要登入會員身分");
        return ResponseEntity.ok(new ReviewImageUploadResponse(images.upload(files, member.memberId())));
    }
}
//review-end，總共1次修改，第1次//
