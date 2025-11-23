package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mapper.BookInfoMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Service
public class BookCoverService {

    @Value("${book.cover.base-dir:covers}")
    private String baseDir;

    @Value("${book.cover.url-prefix:/covers}")
    private String urlPrefix;

    @Autowired
    private BookInfoMapper bookInfoMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String COVER_CACHE_PREFIX = "book:cover:"; // + id
    private static final long COVER_CACHE_MINUTES = 30;
    private static final Set<String> ALLOWED_EXT = new HashSet<>(java.util.Arrays.asList(".png", ".jpg", ".jpeg", ".webp"));
    private static final Set<String> ALLOWED_CONTENT_TYPES = new HashSet<>(java.util.Arrays.asList("image/png", "image/jpeg", "image/webp"));

    @jakarta.annotation.PostConstruct
    public void initLog() {
        File dir = new File(baseDir);
        System.out.println("[BookCoverService] baseDir configured as: " + dir.getAbsolutePath());
        if (!dir.exists()) {
            System.out.println("[BookCoverService] directory does not exist, will be created on first upload.");
        }
        String os = System.getProperty("os.name").toLowerCase();
        if ((os.contains("linux") || os.contains("unix")) && baseDir.contains(":" ) && !dir.exists()) {
            // 优先选择容器已挂载的 /sources/covers 作为共享卷路径，其次 /usr/share/nginx/html/covers（若存在）
            File sharedSources = new File("/sources/covers");
            File nginxStatic = new File("/usr/share/nginx/html/covers");
            String fallback;
            if (sharedSources.exists()) {
                fallback = sharedSources.getAbsolutePath();
            } else if (nginxStatic.exists()) {
                fallback = nginxStatic.getAbsolutePath();
            } else {
                // 如果都不存在，仍使用 /sources/covers 并创建，保证与 docker-compose 映射保持一致
                fallback = sharedSources.getAbsolutePath();
                sharedSources.mkdirs();
            }
            System.out.println("[BookCoverService] WARN: Windows style path on Linux, switching baseDir to: " + fallback);
            baseDir = fallback;
        }
    }

    /**
     * 获取书籍封面URL（使用Redis做短期缓存）
     */
    public String getCoverUrl(Integer bookId) {
        if (bookId == null) return null;
        String key = COVER_CACHE_PREFIX + bookId;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof String) {
            return (String) cached;
        }
        String url = bookInfoMapper.queryBookCoverById(bookId);
        if (url == null || url.isBlank()) {
            url = "default.png"; // 兜底
        }
        redisTemplate.opsForValue().set(key, url, COVER_CACHE_MINUTES, TimeUnit.MINUTES);
        return url;
    }

    /**
     * 更新书籍封面（保存文件 + 更新数据库 + 刷新缓存）
     */
    public String uploadCover(Integer bookId, MultipartFile file) throws IOException {
        if (bookId == null) throw new IllegalArgumentException("bookId 不能为空");
        if (file == null || file.isEmpty()) {
            // 使用默认封面
            bookInfoMapper.updateCoverUrlById(bookId, "default.png");
            redisTemplate.opsForValue().set(COVER_CACHE_PREFIX + bookId, "default.png", COVER_CACHE_MINUTES, TimeUnit.MINUTES);
            return buildPublicUrl("default.png");
        }
        // 确保目录存在
        File dir = new File(baseDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.')).toLowerCase();
        }
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("不支持的图片类型: " + contentType);
        }
        if (!ext.isEmpty() && !ALLOWED_EXT.contains(ext)) {
            throw new IllegalArgumentException("扩展名不在允许范围: " + ext);
        }
        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String filename = "book_" + bookId + "_" + ts + "_" + UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = Path.of(baseDir, filename);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        // 更新数据库记录为相对路径（文件名）
        bookInfoMapper.updateCoverUrlById(bookId, filename);
        // 刷新缓存
        redisTemplate.opsForValue().set(COVER_CACHE_PREFIX + bookId, filename, COVER_CACHE_MINUTES, TimeUnit.MINUTES);
        return buildPublicUrl(filename);
    }

    private String buildPublicUrl(String name) {
        if (name == null) name = "default.png";
        if (name.startsWith("http://") || name.startsWith("https://")) {
            return name; // 已经是绝对URL
        }
        String p = urlPrefix;
        if (!p.endsWith("/")) p += "/";
        return p + name;
    }
}
