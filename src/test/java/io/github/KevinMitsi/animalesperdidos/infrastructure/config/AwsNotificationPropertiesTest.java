package io.github.KevinMitsi.animalesperdidos.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AwsNotificationPropertiesTest {
    @Test
    void rejectsIncompleteAwsConfiguration() {
        AwsNotificationProperties properties = new AwsNotificationProperties();
        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void acceptsCompleteAwsConfiguration() {
        AwsNotificationProperties properties = new AwsNotificationProperties();
        properties.setQueueUrl("https://sqs.us-east-1.amazonaws.com/123/notifications");
        properties.setSenderEmail("no-reply@example.com");
        properties.setPlatformApplicationArn("arn:aws:sns:us-east-1:123:app/GCM/animals");
        assertDoesNotThrow(properties::validate);
    }
}
