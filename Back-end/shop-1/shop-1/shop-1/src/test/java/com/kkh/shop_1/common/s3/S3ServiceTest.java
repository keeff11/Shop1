package com.kkh.shop_1.common.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * * S3Service 단위 테스트
 * * 핵심 검증 대상: 클라이언트가 보낸 파일명/Content-Type을 신뢰하지 않고
 *   Apache Tika로 실제 바이트를 검사해 이미지 위변조 업로드를 차단하는 보안 로직
 */
@ExtendWith(MockitoExtension.class)
class S3ServiceTest {

    // PNG 매직 바이트 시그니처 (89 50 4E 47 0D 0A 1A 0A)
    private static final byte[] PNG_MAGIC_BYTES = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    // JPEG 매직 바이트 시그니처 (FF D8 FF)
    private static final byte[] JPEG_MAGIC_BYTES = new byte[]{
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
    };

    private S3Service s3Service;

    @Mock
    private AmazonS3 amazonS3;

    @BeforeEach
    void setUp() {
        s3Service = new S3Service();
    }

    @Nested
    @DisplayName("업로드 전 유효성 검증")
    class ValidationOnly {

        @Test
        @DisplayName("null 파일 업로드 시 예외가 발생한다")
        void uploadImage_nullFile_throwsException() {
            assertThatThrownBy(() -> s3Service.uploadImage("items", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("업로드할 이미지가 없습니다.");
        }

        @Test
        @DisplayName("빈 파일 업로드 시 예외가 발생한다")
        void uploadImage_emptyFile_throwsException() {
            MockMultipartFile emptyFile =
                    new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

            assertThatThrownBy(() -> s3Service.uploadImage("items", emptyFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("업로드할 이미지가 없습니다.");
        }

        @Test
        @DisplayName("10MB를 초과하는 파일 업로드 시 예외가 발생한다")
        void uploadImage_oversizedFile_throwsException() {
            // MockMultipartFile 대신 Mockito mock으로 실제 바이트 할당 없이 getSize()만 큰 값으로 스텁
            MultipartFile oversizedFile = mock(MultipartFile.class);
            given(oversizedFile.isEmpty()).willReturn(false);
            given(oversizedFile.getSize()).willReturn(11L * 1024 * 1024);

            assertThatThrownBy(() -> s3Service.uploadImage("items", oversizedFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("실제 내용은 이미지가 아니지만 파일명/Content-Type을 이미지로 위장한 경우 업로드가 거부된다")
        void uploadImage_spoofedContentType_throwsException() {
            // 파일명은 evil.png, 선언된 Content-Type은 image/png 이지만 실제 바이트는 텍스트다.
            // Tika가 실제 바이트를 검사해 text/plain을 탐지하므로 신뢰할 수 없는 클라이언트 메타데이터는 무시되어야 한다.
            MockMultipartFile spoofedFile = new MockMultipartFile(
                    "image", "evil.png", "image/png",
                    "this is not an image".getBytes(StandardCharsets.UTF_8)
            );

            assertThatThrownBy(() -> s3Service.uploadImage("items", spoofedFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미지 파일(jpg, png, gif, webp)만 업로드할 수 있습니다.");
        }
    }

    @Nested
    @DisplayName("업로드 성공 (S3 호출 도달)")
    class UploadSuccess {

        @BeforeEach
        void mockS3Client() throws Exception {
            ReflectionTestUtils.setField(s3Service, "s3", amazonS3);
            ReflectionTestUtils.setField(s3Service, "bucketName", "test-bucket");

            given(amazonS3.putObject(any(PutObjectRequest.class))).willReturn(mock(PutObjectResult.class));
            given(amazonS3.getUrl(eq("test-bucket"), anyString()))
                    .willReturn(new URL("https://cdn.example.com/test-bucket/mock-key"));
        }

        @Test
        @DisplayName("실제 PNG 바이트를 가진 파일은 검증을 통과하고 S3 업로드를 호출한다")
        void uploadImage_realPng_success() throws Exception {
            MockMultipartFile pngFile =
                    new MockMultipartFile("image", "photo.png", "image/png", PNG_MAGIC_BYTES);

            String url = s3Service.uploadImage("items", pngFile);

            assertThat(url).isEqualTo("https://cdn.example.com/test-bucket/mock-key");
            verify(amazonS3, times(1)).putObject(any(PutObjectRequest.class));
        }

        @Test
        @DisplayName("실제 JPEG 바이트를 가진 파일은 검증을 통과하고 S3 업로드를 호출한다")
        void uploadImage_realJpeg_success() throws Exception {
            MockMultipartFile jpegFile =
                    new MockMultipartFile("image", "photo.jpg", "image/jpeg", JPEG_MAGIC_BYTES);

            String url = s3Service.uploadImage("items", jpegFile);

            assertThat(url).isEqualTo("https://cdn.example.com/test-bucket/mock-key");
            verify(amazonS3, times(1)).putObject(any(PutObjectRequest.class));
        }
    }
}
