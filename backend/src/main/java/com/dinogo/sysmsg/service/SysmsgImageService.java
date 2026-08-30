package com.dinogo.sysmsg.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.dinogo.sysmsg.exception.SysmsgConflictException;

//sysmsg-start，總共1次修改，第1次//
/** 功能：上傳訊息圖片至 Cloudinary；應用：資料庫只保存 secure_url。 */
@Service
public class SysmsgImageService {
    private static final List<String> ALLOWED = List.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private final Cloudinary cloudinary;
    public SysmsgImageService(Cloudinary cloudinary) { this.cloudinary = cloudinary; }

    public List<String> upload(List<MultipartFile> files, Integer memberId) {
        if (files == null || files.isEmpty() || files.size() > 3) throw new IllegalArgumentException("訊息圖片每次須上傳 1 至 3 張");
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) throw new IllegalArgumentException("訊息圖片不可為空檔案");
            if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("單張訊息圖片不可超過 10MB");
            if (!ALLOWED.contains(file.getContentType())) throw new IllegalArgumentException("訊息圖片只接受 PNG、JPEG、GIF 或 WebP");
            try {
                Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                        "folder", "dinogo/sysmsg/" + memberId, "resource_type", "image"));
                String url = String.valueOf(result.get("secure_url"));
                if (!url.startsWith("https://res.cloudinary.com/")) throw new SysmsgConflictException("Cloudinary 未回傳安全圖片網址");
                urls.add(url);
            } catch (IOException exception) {
                throw new SysmsgConflictException("Cloudinary 訊息圖片上傳失敗");
            }
        }
        return List.copyOf(urls);
    }
}
//sysmsg-end，總共1次修改，第1次//
