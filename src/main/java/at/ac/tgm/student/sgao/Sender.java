package at.ac.tgm.student.sgao;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sender {

    Logger logger = LoggerFactory.getLogger(Sender.class);

    private static String user = ActiveMQConnection.DEFAULT_USER;
    private static String password = ActiveMQConnection.DEFAULT_PASSWORD;
    private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;

    private Connection connection = null;
    private Session session = null;
    private MessageProducer producer = null;

    private final String queue;

    public Sender(String queue) {
        this.queue = queue;
        logger.info("Sender started on queue: " + queue);

        Destination destination;
        try {

            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory( user, password, url );
            connection = connectionFactory.createConnection();
            connection.start();

            // Create the session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue( this.queue );

            // Create the producer.
            producer = session.createProducer(destination);
            producer.setDeliveryMode( DeliveryMode.NON_PERSISTENT );

        } catch (Exception e) {
            System.out.println("[MessageProducer] Caught: " + e);
            e.printStackTrace();
        }
    }

    public void sendMessageToQueue(WarehouseData message) {
        try {
            ObjectMessage objectMessage = session.createObjectMessage(message);
            producer.send(objectMessage);

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try { producer.close(); } catch ( Exception e ) {}
        try { session.close(); } catch ( Exception e ) {}
        try { connection.close(); } catch ( Exception e ) {}
    }
}
