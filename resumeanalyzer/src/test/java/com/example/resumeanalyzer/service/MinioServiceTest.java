package com.example.resumeanalyzer.service;

import io.minio.*;
import io.minio.http.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceTest {

    @Mock
    private MinioClient minioClient;

    @Mock
    private MultipartFile multipartFile;

    @InjectMocks
    private MinioService minioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "bucketName", "test-bucket");
    }

    @Test
    void uploadFile_Success_WhenBucketExists() throws Exception {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test-resume.pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(multipartFile.getSize()).thenReturn(12L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // Act
        String result = minioService.uploadFile(multipartFile);

        // Assert
        assertNotNull(result);
        assertTrue(result.endsWith("_test-resume.pdf"));
        verify(minioClient, times(1)).bucketExists(any(BucketExistsArgs.class));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
    }

    @Test
    void uploadFile_Success_WhenBucketNotExists() throws Exception {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test-resume.pdf");
        when(multipartFile.getInputStream()).thenReturn(new ByteArrayInputStream("test content".getBytes()));
        when(multipartFile.getSize()).thenReturn(12L);
        when(multipartFile.getContentType()).thenReturn("application/pdf");
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);

        // Act
        String result = minioService.uploadFile(multipartFile);

        // Assert
        assertNotNull(result);
        verify(minioClient, times(1)).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    void uploadFile_ThrowsException_WhenMinioFails() throws Exception {
        // Arrange
        when(multipartFile.getOriginalFilename()).thenReturn("test.pdf");
        when(minioClient.bucketExists(any(BucketExistsArgs.class)))
                .thenThrow(new RuntimeException("MinIO connection failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            minioService.uploadFile(multipartFile);
        });
        assertTrue(exception.getMessage().contains("Error uploading file to MinIO"));
    }

    @Test
    void getFileUrl_Success() throws Exception {
        // Arrange
        String fileName = "test-file.pdf";
        String expectedUrl = "http://minio:9000/test-bucket/test-file.pdf?signed=true";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn(expectedUrl);

        // Act
        String result = minioService.getFileUrl(fileName);

        // Assert
        assertEquals(expectedUrl, result);
        verify(minioClient, times(1)).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
    }

    @Test
    void getFileUrl_ThrowsException_WhenMinioFails() throws Exception {
        // Arrange
        String fileName = "test-file.pdf";
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenThrow(new RuntimeException("MinIO error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            minioService.getFileUrl(fileName);
        });
        assertTrue(exception.getMessage().contains("Error getting file URL from MinIO"));
    }
}
