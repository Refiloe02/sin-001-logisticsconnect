package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import org.apache.activemq.ActiveMQConnectionFactory;

public final class StageSubscriber implements AutoCloseable {

    public static final String CLIENT_ID = "transit-service";
    public static final String SUBSCRIPTION_NAME = "transit-service-eta";

    private final ActiveMQConnectionFactory factory;
    private final StageCache cache;
    private final long retryMillis;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile Connection connection;
    private volatile boolean closed;

    public StageSubscriber(String brokerUrl, StageCache cache) {
        this(brokerUrl, cache, 5000);
    }

    public StageSubscriber(String brokerUrl, StageCache cache, long retryMillis) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
        this.cache = cache;
        this.retryMillis = retryMillis;
    }

     public void start() {
        Thread thread = new Thread(this::connectWithRetry, "stage-subscriber");
        thread.setDaemon(true);
        thread.start();
    }

    public boolean isConnected() {
        return connection != null;
    }

    private void connectWithRetry() {
        while (!closed) {
            try {
                connectOnce();
                return;
            } catch (JMSException e) {
                System.err.println("transit-service: broker not reachable (" + e.getMessage()
                        + "), retrying in " + retryMillis + " ms");
                try {
                    Thread.sleep(retryMillis);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void connectOnce() throws JMSException {
        Connection c = factory.createConnection();
        try {
            c.setClientID(CLIENT_ID);                          // must be set before anything else
            c.setExceptionListener(e -> onConnectionLost(c, e));
            Session session = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic(MqConfig.TOPIC);
            MessageConsumer consumer = session.createDurableSubscriber(topic, SUBSCRIPTION_NAME);
            consumer.setMessageListener(this::onMessage);
            c.start();
            connection = c;
            System.out.println("transit-service subscribed to " + MqConfig.TOPIC);
        } catch (JMSException e) {
            closeQuietly(c);
            throw e;
        }
    }

    private void onConnectionLost(Connection lost, JMSException cause) {
        if (closed) {
            return;
        }
        System.err.println("transit-service lost the broker connection: " + cause.getMessage());
        connection = null;
        closeQuietly(lost);
        Thread reconnect = new Thread(this::connectWithRetry, "stage-subscriber-reconnect");
        reconnect.setDaemon(true);
        reconnect.start();
    }

    void onMessage(Message message) {
        try {
            if (!(message instanceof TextMessage text)) {
                System.err.println("transit-service ignored a non-text message");
                return;
            }
            StageChange change = mapper.readValue(text.getText(), StageChange.class);
            if (change.hubId() == null || change.stage() == null || change.stage() < 0 || change.stage() > 8) {
                System.err.println("transit-service ignored an invalid stage message: " + text.getText());
                return;
            }
            cache.update(change.hubId(), change.stage());
            System.out.println("transit-service cached " + change.hubId() + " -> stage " + change.stage());
        } catch (Exception e) {
            System.err.println("transit-service ignored a malformed message: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        closed = true;
        Connection c = connection;
        connection = null;
        if (c != null) {
            closeQuietly(c);
        }
    }

    private static void closeQuietly(Connection c) {
        try {
            c.close();
        } catch (JMSException ignored) {
            // already broken - nothing to do
        }
    }
}
