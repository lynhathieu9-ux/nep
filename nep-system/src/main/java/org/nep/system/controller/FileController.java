package org.nep.system.controller;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.nep.common.result.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

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

    @Operation(summary = "上传头像（Minio存储）")
    @PostMapping("/avatar")
    public Result<String> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return Result.fail("文件为空");
        if (file.getSize() > 5 * 1024 * 1024) return Result.fail("文件大小不能超过5MB");

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return Result.fail("只允许上传图片文件");

        try {
            MinioClient client = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            String ext = getExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + ext;

            PutObjectArgs args = PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(filename)
                    .contentType(contentType)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build();

            client.putObject(args);

            String url = String.format("%s/%s/%s", endpoint, bucket, filename);
            return Result.ok("上传成功", url);
        } catch (Exception e) {
            return Result.fail("上传失败: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        return filename.substring(filename.lastIndexOf("."));
    }
}
