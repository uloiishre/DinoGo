package com.dinogo.review.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.review.exception.ReviewConflictException;

//review-start，總共1次修改，第1次//
/** 功能：驗證並上傳評論圖片；應用：資料庫只保存 Cloudinary HTTPS URL。 */
@Service
public class ReviewImageService {
    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private final Cloudinary cloudinary;
    public ReviewImageService(Cloudinary cloudinary) { this.cloudinary = cloudinary; }

    public List<String> upload(List<MultipartFile> files, Integer memberId) {
        if (files == null || files.isEmpty() || files.size() > 3) throw new IllegalArgumentException("評論圖片每次須上傳 1 至 3 張");
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            validate(file);
            try {
                Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "dinogo/reviews/" + memberId, "resource_type", "image"));
                String url = String.valueOf(result.get("secure_url"));
                if (!url.startsWith("https://res.cloudinary.com/")) throw new ReviewConflictException("Cloudinary 未回傳安全圖片網址");
                urls.add(url);
            } catch (IOException exception) {
                throw new ReviewConflictException("Cloudinary 評論圖片上傳失敗", exception);
            }
        }
        return List.copyOf(urls);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("評論圖片不可為空檔案");
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("單張評論圖片不可超過 10MB");
        if (!ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("評論圖片只接受 PNG、JPEG、GIF 或 WebP");
    }
}
//review-end，總共1次修改，第1次//
