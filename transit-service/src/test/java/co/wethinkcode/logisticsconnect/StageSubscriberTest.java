package co.wethinkcode.logisticsconnect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Runs against a real in-JVM ActiveMQ broker on a random TCP port (no Docker, no clash with a local broker). */
class StageSubscriberTest {

    private BrokerService broker;
    private String brokerUrl;

    @BeforeEach
    void startBroker() throws Exception {
        broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.setBrokerName("transit-test");
        brokerUrl = broker.addConnector("tcp://127.0.0.1:0").getPublishableConnectString();
        broker.start();
        broker.waitUntilStarted();
    }

    @AfterEach
    void stopBroker() throws Exception {
        broker.stop();
        broker.waitUntilStopped();
    }

    private void publish(String json) throws Exception {
        Connection c = new ActiveMQConnectionFactory(brokerUrl).createConnection();
        try {
            Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer p = s.createProducer(s.createTopic(MqConfig.TOPIC));
            p.send(s.createTextMessage(json));
        } finally {
            c.close();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
        assertTrue(condition.getAsBoolean(), "condition not met within 5s");
    }

    @Test
    void aPublishedMessageUpdatesTheCache() throws Exception {
        StageCache cache = new StageCache();
        try (StageSubscriber subscriber = new StageSubscriber(brokerUrl, cache, 100)) {
            subscriber.start();
            waitUntil(subscriber::isConnected);

            publish("{\"hubId\":\"H-501\",\"stage\":5,\"timestamp\":\"2026-07-18T10:15:00Z\"}");

            waitUntil(() -> cache.stageFor("H-501") == 5);
        }
    }

    @Test
    void messagesPublishedWhileOfflineAreDeliveredOnReconnect() throws Exception {
        StageCache cache = new StageCache();
        StageSubscriber first = new StageSubscriber(brokerUrl, cache, 100);
        first.start();
        waitUntil(first::isConnected);
        first.close();                                           // transit-service goes down...

        publish("{\"hubId\":\"H-503\",\"stage\":7,\"timestamp\":\"2026-07-18T11:00:00Z\"}");   // ...while this is sent
        assertEquals(0, cache.stageFor("H-503"));

        try (StageSubscriber second = new StageSubscriber(brokerUrl, cache, 100)) {              // ...and comes back
            second.start();
            waitUntil(() -> cache.stageFor("H-503") == 7);
        }
    }

    @Test
    void malformedMessagesAreIgnoredAndTheSubscriberKeepsWorking() throws Exception {
        StageCache cache = new StageCache();
        try (StageSubscriber subscriber = new StageSubscriber(brokerUrl, cache, 100)) {
            subscriber.start();
            waitUntil(subscriber::isConnected);

            publish("not json at all");
            publish("{\"hubId\":\"H-501\",\"stage\":99}");           // out of range
            publish("{\"hubId\":\"H-501\",\"stage\":4}");

            waitUntil(() -> cache.stageFor("H-501") == 4);
        }
    }
}
