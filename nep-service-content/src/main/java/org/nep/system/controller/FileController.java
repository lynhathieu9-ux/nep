package org.nep.system.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.nep.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 文件上传控制器
 * <p>
 * 双模存储策略：
 * <ol>
 *   <li>优先使用 MinIO 对象存储（生产环境）</li>
 *   <li>MinIO 不可用时自动降级为本地存储（开发环境 / 无Docker场景）</li>
 * </ol>
 *
 * @author NEP Team
 */
@Slf4j
@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket}")
    private String bucket;

    /** 本地上传根目录（与 WebConfig 的静态资源映射保持一致） */
    private static final String LOCAL_UPLOAD_DIR = System.getProperty("user.dir") + File.separator + "uploads";

    /** 是否已检测 MinIO 可用（避免每次上传都尝试连接超时） */
    private volatile boolean minioAvailable = true;
    private volatile long lastMinioCheck = 0;
    private static final long MINIO_CHECK_INTERVAL_MS = 30_000; // 30秒检查一次

    @Operation(summary = "上传头像（MinIO优先，自动降级本地存储）")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > 5 * 1024 * 1024) return Result.fail("文件大小不能超过5MB");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return Result.fail("只允许上传图片文件");

        String ext = getExtension(file.getOriginalFilename());
        String filename = "avatar/" + UUID.randomUUID() + ext;

        // 1. 尝试 MinIO
        if (isMinioAvailable()) {
            try {
                String url = uploadToMinio(file, filename, contentType);
                return Result.ok("上传成功", url);
            } catch (Exception e) {
                log.warn("MinIO 上传失败，降级为本地存储: {}", e.getMessage());
                minioAvailable = false;
                lastMinioCheck = System.currentTimeMillis();
            }
        }

        // 2. 降级为本地存储
        try {
            String url = uploadToLocal(file, filename);
            return Result.ok("上传成功（本地存储）", url);
        } catch (IOException e) {
            log.error("本地文件保存失败", e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    // ==================== 问题④：通用图片上传（环保资讯等） ====================

    @Operation(summary = "上传图片（环保资讯配图等，MinIO优先降级本地，问题④）")
    @PostMapping("/image")
    public Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > 5 * 1024 * 1024) return Result.fail("图片大小不能超过5MB");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return Result.fail("只允许上传图片文件");

        String ext = getExtension(file.getOriginalFilename());
        String filename = "news/" + UUID.randomUUID() + ext;
        return doUpload(file, filename, contentType);
    }

    // ==================== 问题⑤：知识库附件上传 + 下载 ====================

    @Operation(summary = "上传知识库附件（支持pdf/doc等，问题⑤）")
    @PostMapping("/doc")
    public Result<String> uploadDoc(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > 50 * 1024 * 1024) return Result.fail("文件大小不能超过50MB");
        String ext = getDocExtension(file.getOriginalFilename());
        String filename = "doc/" + UUID.randomUUID() + ext;
        return doUpload(file, filename, file.getContentType());
    }

    /**
     * 问题⑤：文件下载。从本地 uploads 目录按相对路径读取文件流并以附件形式响应。
     * <p>path 支持传 "/images/doc/xxx.pdf" 或 "doc/xxx.pdf"，做了目录穿越防护。</p>
     */
    @Operation(summary = "文件下载（知识库附件等，问题⑤）")
    @GetMapping("/download")
    public ResponseEntity<Resource> download(@RequestParam("path") String path,
                                             @RequestParam(value = "name", required = false) String downloadName) {
        // 归一化相对路径：去掉 /images/ 前缀，防目录穿越
        String rel = path.replace("\\", "/");
        if (rel.startsWith("/images/")) rel = rel.substring("/images/".length());
        if (rel.startsWith("images/")) rel = rel.substring("images/".length());
        if (rel.contains("..")) {
            return ResponseEntity.badRequest().build();
        }
        Path file = Paths.get(LOCAL_UPLOAD_DIR, rel).normalize();
        // 二次校验：必须仍在 uploads 目录内
        if (!file.startsWith(Paths.get(LOCAL_UPLOAD_DIR))) {
            return ResponseEntity.badRequest().build();
        }
        if (!Files.exists(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file.toFile());
        String fname = (downloadName != null && !downloadName.isEmpty())
                ? downloadName : file.getFileName().toString();
        String encoded = URLEncoder.encode(fname, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    /** 抽取的双模上传：MinIO 优先，失败降级本地 */
    private Result<String> doUpload(MultipartFile file, String filename, String contentType) {
        if (isMinioAvailable()) {
            try {
                String url = uploadToMinio(file, filename, contentType);
                return Result.ok("上传成功", url);
            } catch (Exception e) {
                log.warn("MinIO 上传失败，降级为本地存储: {}", e.getMessage());
                minioAvailable = false;
                lastMinioCheck = System.currentTimeMillis();
            }
        }
        try {
            String url = uploadToLocal(file, filename);
            return Result.ok("上传成功（本地存储）", url);
        } catch (IOException e) {
            log.error("本地文件保存失败", e);
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    private String getDocExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".dat";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    // ==================== MinIO 上传 ====================

    private String uploadToMinio(MultipartFile file, String filename, String contentType) throws Exception {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        PutObjectArgs args = PutObjectArgs.builder()
                .bucket(bucket)
                .object(filename)
                .contentType(contentType)
                .stream(file.getInputStream(), file.getSize(), -1)
                .build();

        client.putObject(args);
        return String.format("%s/%s/%s", endpoint, bucket, filename);
    }

    private boolean isMinioAvailable() {
        long now = System.currentTimeMillis();
        if (!minioAvailable && (now - lastMinioCheck) < MINIO_CHECK_INTERVAL_MS) {
            return false; // 短路：30秒内不再重试
        }
        // 允许重试
        lastMinioCheck = now;
        return true;
    }

    // ==================== 本地存储 ====================

    /**
     * 保存文件到本地 uploads 目录
     * <p>
     * 存储路径：{user.dir}/uploads/avatar/{uuid}.{ext}
     * 访问URL：/images/avatar/{uuid}.{ext}
     */
    private String uploadToLocal(MultipartFile file, String filename) throws IOException {
        Path targetPath = Paths.get(LOCAL_UPLOAD_DIR, filename);
        // 确保父目录存在
        Files.createDirectories(targetPath.getParent());
        // 保存文件
        file.transferTo(targetPath.toFile());

        // 返回可通过静态资源映射访问的URL
        String url = "/images/" + filename;
        log.info("文件已保存到本地: {} -> {}", targetPath, url);
        return url;
    }

    // ==================== 工具方法 ====================

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        String ext = filename.substring(filename.lastIndexOf(".")).toLowerCase();
        // 只允许常见图片格式
        return switch (ext) {
            case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp" -> ext;
            default -> ".png";
        };
    }
}
