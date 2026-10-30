package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.jms.Connection;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

/** Proves a message really travels through a broker. Uses an embedded in-JVM broker (vm://), no Docker needed. */
class ActiveMqStagePublisherTest {

    private static final String EMBEDDED = "vm://stage-test?broker.persistent=false&broker.useJmx=false";

    @Test
    void publishedStageChangeArrivesOnThePackageStatusTopic() throws Exception {
        Connection connection = new ActiveMQConnectionFactory(EMBEDDED).createConnection();
        try {
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer subscriber = session.createConsumer(session.createTopic(MqConfig.TOPIC));

            try (ActiveMqStagePublisher publisher = new ActiveMqStagePublisher(EMBEDDED)) {
                publisher.publish(new StageChange("H-501", 5, "2026-07-18T10:15:00Z"));
            }

            TextMessage received = (TextMessage) subscriber.receive(5000);
            assertNotNull(received, "no message arrived on " + MqConfig.TOPIC);
            JsonNode json = new ObjectMapper().readTree(received.getText());
            assertEquals("H-501", json.get("hubId").asText());
            assertEquals(5, json.get("stage").asInt());
            assertEquals("2026-07-18T10:15:00Z", json.get("timestamp").asText());
        } finally {
            connection.close();
        }
    }
}
