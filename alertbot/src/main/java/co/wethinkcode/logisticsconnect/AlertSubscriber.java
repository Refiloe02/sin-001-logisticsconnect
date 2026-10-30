package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.TextMessage;
import org.apache.activemq.ActiveMQConnectionFactory;

public final class AlertSubscriber implements AutoCloseable {

    public static final String CLIENT_ID = "alertbot";
    public static final String SUBSCRIPTION_NAME = "alertbot-alerts";

    private final ActiveMQConnectionFactory factory;
    private final Consumer<StageChange> handler;
    private final long retryMillis;
    private final ObjectMapper mapper = new ObjectMapper();
    private volatile Connection connection;
    private volatile boolean closed;

    public AlertSubscriber(String brokerUrl, Consumer<StageChange> handler, long retryMillis) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
        this.handler = handler;
        this.retryMillis = retryMillis;
    }

    public void start() {
        Thread thread = new Thread(this::connectWithRetry, "alert-subscriber");
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
                System.err.println("alertbot: broker not reachable (" + e.getMessage() + "), retrying in " + retryMillis + " ms");
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
            c.setClientID(CLIENT_ID);
            c.setExceptionListener(e -> onConnectionLost(c, e));
            Session session = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createDurableSubscriber(session.createTopic(MqConfig.TOPIC), SUBSCRIPTION_NAME);
            consumer.setMessageListener(this::onMessage);
            c.start();
            connection = c;
            System.out.println("alertbot subscribed to " + MqConfig.TOPIC);
        } catch (JMSException e) {
            closeQuietly(c);
            throw e;
        }
    }

    private void onConnectionLost(Connection lost, JMSException cause) {
        if (closed) {
            return;
        }
        System.err.println("alertbot lost the broker connection: " + cause.getMessage());
        connection = null;
        closeQuietly(lost);
        Thread reconnect = new Thread(this::connectWithRetry, "alert-subscriber-reconnect");
        reconnect.setDaemon(true);
        reconnect.start();
    }

    void onMessage(Message message) {
        try {
            if (message instanceof TextMessage text) {
                handler.accept(mapper.readValue(text.getText(), StageChange.class));
            }
        } catch (Exception e) {
            System.err.println("alertbot ignored a malformed message: " + e.getMessage());
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
            // already broken
        }
    }
}
