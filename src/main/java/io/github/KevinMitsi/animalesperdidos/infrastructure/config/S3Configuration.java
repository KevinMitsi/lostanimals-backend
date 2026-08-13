package io.github.kevinmitsi.animalesperdidos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;

@Configuration
public class S3Configuration {

    @Bean
    @ConfigurationProperties("app.storage.s3")
    S3Properties s3Properties() {
        return new S3Properties();
    }

    @Bean(destroyMethod = "close")
    S3AsyncClient s3AsyncClient(S3Properties properties) {
        return S3AsyncClient.builder().region(Region.of(properties.getRegion())).build();
    }

    public static class S3Properties {
        private String bucket;
        private String region = "us-east-1";

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }
}
