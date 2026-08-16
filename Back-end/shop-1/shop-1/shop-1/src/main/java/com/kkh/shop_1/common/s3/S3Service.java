package com.kkh.shop_1.common.s3;

import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Logger 대신 Slf4j 어노테이션 사용
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp"
    );

    @Value("${spring.s3.endpoint}")
    private String endPoint;

    @Value("${spring.s3.region}")
    private String region;

    @Value("${spring.s3.accessKey}")
    private String accessKey;

    @Value("${spring.s3.secretKey}")
    private String secretKey;

    @Value("${spring.s3.bucket}")
    private String bucketName;

    private AmazonS3 s3;
    private final Tika tika = new Tika();

    @PostConstruct
    public void init() {
        this.s3 = AmazonS3ClientBuilder.standard()
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endPoint, region))
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
                .build();
    }

    /**
     *
     * MultipartFile 이미지 업로드
     *
     */
    public String uploadImage(String folderPath, MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
        }

        // 클라이언트가 보낸 파일명/Content-Type을 신뢰하지 않고, 실제 파일 내용으로 이미지 여부를 검증한다.
        byte[] bytes = image.getBytes();
        String contentType = tika.detect(bytes);
        if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("이미지 파일(jpg, png, gif, webp)만 업로드할 수 있습니다.");
        }

        return uploadImageFromBytes(folderPath, bytes);
    }

    /**
     *
     * byte[] 이미지 업로드
     *
     */
    public String uploadImageFromBytes(String folderPath, byte[] bytes) throws IOException {
        String contentType = tika.detect(bytes);
        String extension = getExtensionFromContentType(contentType);
        String key = createKey(folderPath, extension);

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(bytes.length);
        metadata.setContentType(contentType);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes)) {
            return uploadToS3(key, inputStream, metadata);
        }
    }

    /**
     *
     * (Private) 공통 S3 업로드 로직
     *
     */
    private String uploadToS3(String key, InputStream inputStream, ObjectMetadata metadata) throws IOException {
        try {
            s3.putObject(new PutObjectRequest(bucketName, key, inputStream, metadata)
                    .withCannedAcl(CannedAccessControlList.PublicRead));

            log.info("S3 업로드 성공: {}", key);
            return s3.getUrl(bucketName, key).toString();
        } catch (SdkClientException e) {
            log.error("S3 업로드 실패: {}", key, e);
            throw new IOException("S3 upload failed", e);
        }
    }

    /**
     *
     * 이미지 삭제 (URL 기준)
     *
     */
    public void deleteImageByUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) return;

        try {
            String key = extractKeyFromUrl(imageUrl);
            s3.deleteObject(bucketName, key);
            log.info("S3 delete success: {}", key);
        } catch (SdkClientException e) {
            log.error("S3 delete failed: {}", imageUrl, e);
            throw new RuntimeException("S3 delete failed", e);
        }
    }

    /**
     *
     * 폴더 및 내부 파일 삭제
     *
     */
    public void deleteFolder(String folderPath) {
        try {
            ObjectListing objectListing = s3.listObjects(bucketName, folderPath);
            List<String> keys = objectListing.getObjectSummaries().stream()
                    .map(S3ObjectSummary::getKey)
                    .collect(Collectors.toList());

            if (keys.isEmpty()) return;

            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucketName)
                    .withKeys(keys.toArray(new String[0]));

            s3.deleteObjects(deleteRequest);
            log.info("S3 folder deleted: {} ({} files)", folderPath, keys.size());

        } catch (SdkClientException e) {
            log.error("S3 folder delete failed: {}", folderPath, e);
            throw new RuntimeException("S3 folder delete failed", e);
        }
    }

    /**
     *
     * 테스트용: 이미지 Base64 인코딩
     *
     */
    public String encodeImageToBase64(String imageUrl) throws IOException {
        try (InputStream inputStream = new URL(imageUrl).openStream()) {
            byte[] bytes = inputStream.readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        }
    }

    private String createKey(String folderPath, String extension) {
        return folderPath + "/" + UUID.randomUUID() + extension;
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return ".bin";
        switch (contentType) {
            case "image/jpeg": return ".jpg";
            case "image/png": return ".png";
            case "image/gif": return ".gif";
            case "image/webp": return ".webp";
            default: return ".bin";
        }
    }

    /**
     *
     * URL에서 S3 Key 추출
     *
     */
    private String extractKeyFromUrl(String imageUrl) {
        try {
            URI uri = new URI(imageUrl);
            String path = uri.getPath();

            path = URLDecoder.decode(path, StandardCharsets.UTF_8);

            if (path.startsWith("/" + bucketName + "/")) {
                return path.substring(bucketName.length() + 2);
            }

            if (path.startsWith("/")) {
                return path.substring(1);
            }
            return path;
        } catch (Exception e) {
            log.error("Failed to extract key from URL: {}", imageUrl, e);
            throw new IllegalArgumentException("Invalid S3 URL", e);
        }
    }
}