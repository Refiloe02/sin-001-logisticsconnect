package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.junit.jupiter.api.Test;

import javax.jms.Connection;
import javax.jms.MessageProducer;
import javax.jms.Session;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlertSubscriberTest {

    @Test
    void aStageAboveTheThresholdOnTheTopicTriggersAnAlert() throws Exception {
        BrokerService broker = new BrokerService();
        broker.setPersistent(false);
        broker.setUseJmx(false);
        broker.setBrokerName("alertbot-test");
        String url = broker.addConnector("tcp://127.0.0.1:0").getPublishableConnectString();
        broker.start();
        try {
            List<Alert> posted = new CopyOnWriteArrayList<>();
            AlertBotService bot = new AlertBotService(new AlertPolicy(5), posted::add);
            try (AlertSubscriber subscriber = new AlertSubscriber(url, bot::handle, 100)) {
                subscriber.start();
                waitUntil(subscriber::isConnected);

                Connection c = new ActiveMQConnectionFactory(url).createConnection();
                Session s = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer p = s.createProducer(s.createTopic(MqConfig.TOPIC));
                p.send(s.createTextMessage("{\"hubId\":\"H-501\",\"stage\":2,\"timestamp\":\"t1\"}"));   // below: silent
                p.send(s.createTextMessage("{\"hubId\":\"H-501\",\"stage\":6,\"timestamp\":\"t2\"}"));   // crosses: alert
                c.close();

                waitUntil(() -> !posted.isEmpty());
                assertEquals(1, posted.size());
                assertEquals("RAISED", posted.get(0).kind());
            }
        } finally {
            broker.stop();
        }
    }

    private static void waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(25);
        }
        assertTrue(condition.getAsBoolean(), "condition not met within 5s");
    }
}
