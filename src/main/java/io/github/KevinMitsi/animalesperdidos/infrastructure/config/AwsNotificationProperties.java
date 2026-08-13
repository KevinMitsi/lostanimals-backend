package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter @Setter
@ConfigurationProperties("app.notifications.aws")
public class AwsNotificationProperties {
    private boolean enabled;
    private String region="us-east-1";
    private String queueUrl;
    private String senderEmail;
    private String frontendBaseUrl="http://localhost:3000";
    private String platformApplicationArn;
    private int maxMessages=10;
    private int waitTimeSeconds=20;
    private int visibilityTimeoutSeconds=60;

    @PostConstruct
    void validate() {
        requireText(region, "AWS_REGION");
        requireText(queueUrl, "AWS_NOTIFICATION_QUEUE_URL");
        requireText(senderEmail, "AWS_SES_SENDER_EMAIL");
        requireText(frontendBaseUrl, "FRONTEND_BASE_URL");
        requireText(platformApplicationArn, "AWS_SNS_PLATFORM_APPLICATION_ARN");
        if (maxMessages < 1 || maxMessages > 10) {
            throw new IllegalStateException("AWS_NOTIFICATION_MAX_MESSAGES must be between 1 and 10");
        }
        if (waitTimeSeconds < 0 || waitTimeSeconds > 20) {
            throw new IllegalStateException("AWS_NOTIFICATION_WAIT_SECONDS must be between 0 and 20");
        }
        if (visibilityTimeoutSeconds < 1 || visibilityTimeoutSeconds > 43_200) {
            throw new IllegalStateException("AWS_NOTIFICATION_VISIBILITY_SECONDS must be between 1 and 43200");
        }
    }

    private static void requireText(String value, String environmentVariable) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(environmentVariable + " is required when AWS notifications are enabled");
        }
    }
}
