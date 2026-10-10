package com.example.filestorageservice.service;

import com.example.filestorageservice.config.StorageProperties;
import com.example.filestorageservice.s3.S3Cluster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Вся "файловая" работа делегирована S3-совместимому хранилищу (кластер MinIO):
 * никаких путей на диске, коллизий имён и прав на файловой системе.
 * Обращения идут через {@link S3Cluster}, который сам выбирает живую ноду.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final S3Cluster cluster;
    private final StorageProperties properties;

    private volatile boolean bucketReady = false;

    public String store(MultipartFile file) throws IOException {
        String objectKey = generateKey(file.getOriginalFilename());
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        // ContentStreamProvider вызывается заново на каждой попытке, поэтому при
        // failover на другую ноду файл уходит целиком, а не с середины.
        ContentStreamProvider content = () -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };

        try {
            ensureBucket();
            cluster.execute(client -> client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket())
                            .key(objectKey)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromContentProvider(content, file.getSize(), contentType)));
        } catch (SdkException e) {
            throw new IOException("S3 putObject failed: " + e.getMessage(), e);
        }

        log.info("Uploaded object {}/{} (original: {}, size: {} bytes)",
                bucket(), objectKey, file.getOriginalFilename(), file.getSize());
        return objectKey;
    }

    public byte[] load(String objectKey) throws IOException {
        try {
            ensureBucket();
            return cluster.execute(client -> client.getObjectAsBytes(
                            GetObjectRequest.builder().bucket(bucket()).key(objectKey).build())
                    .asByteArray());
        } catch (NoSuchKeyException e) {
            throw new IOException("Object not found: " + objectKey);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new IOException("Object not found: " + objectKey);
            }
            throw new IOException("S3 getObject failed: " + e.getMessage(), e);
        } catch (SdkException e) {
            throw new IOException("S3 getObject failed: " + e.getMessage(), e);
        }
    }

    public void delete(String objectKey) throws IOException {
        try {
            ensureBucket();
            if (!exists(objectKey)) {
                throw new IOException("Object not found: " + objectKey);
            }
            cluster.execute(client -> client.deleteObject(
                    DeleteObjectRequest.builder().bucket(bucket()).key(objectKey).build()));
        } catch (SdkException e) {
            throw new IOException("S3 deleteObject failed: " + e.getMessage(), e);
        }
        log.info("Deleted object {}/{}", bucket(), objectKey);
    }

    public List<StoredObject> list() {
        ensureBucket();
        return cluster.execute(client -> client.listObjectsV2Paginator(
                        ListObjectsV2Request.builder().bucket(bucket()).build())
                .contents().stream()
                .map(o -> new StoredObject(o.key(), o.size() != null ? o.size() : 0L, o.lastModified()))
                .toList());
    }

    private boolean exists(String objectKey) {
        try {
            cluster.execute(client -> client.headObject(
                    HeadObjectRequest.builder().bucket(bucket()).key(objectKey).build()));
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    /**
     * Создаёт bucket при первом обращении: ноды MinIO и сервис стартуют параллельно.
     */
    private void ensureBucket() {
        if (bucketReady) {
            return;
        }
        synchronized (this) {
            if (bucketReady) {
                return;
            }
            boolean exists;
            try {
                cluster.execute(client -> client.headBucket(
                        HeadBucketRequest.builder().bucket(bucket()).build()));
                exists = true;
            } catch (S3Exception e) {
                if (e.statusCode() == 404) {
                    exists = false;
                } else {
                    throw e;
                }
            }

            if (!exists) {
                try {
                    cluster.execute(client -> client.createBucket(
                            CreateBucketRequest.builder().bucket(bucket()).build()));
                    log.info("Created bucket {}", bucket());
                } catch (S3Exception e) {
                    if (e.statusCode() != 409) { // 409 = bucket уже создан
                        throw e;
                    }
                }
            }
            bucketReady = true;
        }
    }

    private String bucket() {
        return properties.bucket();
    }

    private String generateKey(String originalName) {
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return UUID.randomUUID() + extension;
    }

    public record StoredObject(String key, long size, Instant lastModified) {
    }
}
