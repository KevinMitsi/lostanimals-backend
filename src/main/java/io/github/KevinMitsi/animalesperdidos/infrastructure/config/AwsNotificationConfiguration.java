package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2AsyncClient;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration @EnableScheduling @EnableConfigurationProperties(AwsNotificationProperties.class)
@ConditionalOnProperty(name="app.notifications.aws.enabled",havingValue="true")
public class AwsNotificationConfiguration {
    @Bean(destroyMethod="close") SqsAsyncClient sqsAsyncClient(AwsNotificationProperties p){
        return SqsAsyncClient.builder().region(Region.of(p.getRegion())).build();
    }
    @Bean(destroyMethod="close") SesV2AsyncClient sesV2AsyncClient(AwsNotificationProperties p){
        return SesV2AsyncClient.builder().region(Region.of(p.getRegion())).build();
    }
    @Bean(destroyMethod="close") SnsAsyncClient snsAsyncClient(AwsNotificationProperties p){
        return SnsAsyncClient.builder().region(Region.of(p.getRegion())).build();
    }
}
