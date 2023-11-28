package at.ac.tgm.student.sgao;

import jakarta.jms.*;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Receiver {

    Logger logger = LoggerFactory.getLogger(Receiver.class);

    private static String user = ActiveMQConnection.DEFAULT_USER;
    private static String password = ActiveMQConnection.DEFAULT_PASSWORD;
    private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;

    private Connection connection = null;
    private Session session = null;
    private MessageConsumer consumer = null;

    private final String queue;

    public Receiver(String queue) {

        this.queue = queue;

        logger.info("Receiver started on queue: " + queue);

        Destination destination;

        try {

            ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
            connection = connectionFactory.createConnection();
            connection.start();

            // Create the session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue( this.queue );

            consumer = session.createConsumer(destination);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try { consumer.close(); } catch ( Exception e ) {}
        try { session.close(); } catch ( Exception e ) {}
        try { connection.close(); } catch ( Exception e ) {}
    }
}
