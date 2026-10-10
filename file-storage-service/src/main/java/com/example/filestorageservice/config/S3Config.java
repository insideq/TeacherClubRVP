package com.example.filestorageservice.config;

import com.example.filestorageservice.s3.S3Cluster;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.util.List;

/**
 * Клиенты S3 — по одному на каждую ноду кластера MinIO.
 *
 * Отдельный клиент на ноду нужен потому, что подпись SigV4 считается в том числе
 * от заголовка Host: переиспользовать один клиент с подменённым адресом нельзя,
 * подпись перестанет совпадать. Работает и с настоящим AWS S3 — тогда endpoint один.
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    @Bean
    public S3Cluster s3Cluster(StorageProperties properties) {
        List<S3Client> nodes = properties.endpoints().stream()
                .map(endpoint -> buildClient(endpoint, properties))
                .toList();
        return new S3Cluster(nodes);
    }

    private S3Client buildClient(String endpoint, StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
                // Без Apache HttpClient и Netty — чтобы не конфликтовать с версиями Spring Boot.
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                // MinIO отдаёт объекты по path-style: http://host:9000/bucket/key
                .forcePathStyle(true)
                .build();
    }
}
