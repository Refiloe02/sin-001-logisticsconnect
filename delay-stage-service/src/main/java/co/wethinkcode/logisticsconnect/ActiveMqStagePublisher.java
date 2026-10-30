package co.wethinkcode.logisticsconnect;

import co.wethinkcode.logisticsconnect.mq.MqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;

public final class ActiveMqStagePublisher implements StagePublisher, AutoCloseable {

    private final ActiveMQConnectionFactory factory;
    private final ObjectMapper mapper = new ObjectMapper();
    private Connection connection;
    private Session session;
    private MessageProducer producer;

    public ActiveMqStagePublisher() {
        this(MqConfig.BROKER_URL);
    }

    public ActiveMqStagePublisher(String brokerUrl) {
        this.factory = new ActiveMQConnectionFactory(brokerUrl);
    }

    @Override
    public synchronized void publish(StageChange change) throws JMSException, JsonProcessingException {
        String json = mapper.writeValueAsString(change);
        try {
            ensureConnected();
            producer.send(session.createTextMessage(json));
        } catch (JMSException e) {
            close();
            throw e;
        }
    }

    private void ensureConnected() throws JMSException {
        if (connection != null) {
            return;
        }
        Connection c = factory.createConnection();
        try {
            c.start();
            session = c.createSession(false, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(session.createTopic(MqConfig.TOPIC));
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            connection = c;
        } catch (JMSException e) {
            try {
                c.close();
            } catch (JMSException ignored) {
                // nothing more we can do
            }
            throw e;
        }
    }

    @Override
    public synchronized void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (JMSException ignored) {
            // closing a broken connection may itself fail; we are discarding it anyway
        } finally {
            connection = null;
            session = null;
            producer = null;
        }
    }
}
