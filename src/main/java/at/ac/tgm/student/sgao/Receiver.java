package at.ac.tgm.student.sgao;

import jakarta.jms.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.ac.tgm.student.sgao.data.WarehouseData;

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

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
            connectionFactory.setTrustedPackages(List.of("at.ac.tgm.student.sgao.data", "java.util"));
            connection = connectionFactory.createConnection();
            connection.start();

            // Create the session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            destination = session.createQueue( this.queue );

            consumer = session.createConsumer(destination);


        } catch (Exception e) {
            logger.error(e.toString());
        }
    }

    /**
     * Receive all messages in form of WarehouseData and put them in a list.
     * @return a list of received WarehouseData messages
     */
    public List<WarehouseData> receiveMessages() {
        List<WarehouseData> out = new ArrayList<>();
        try {
            // Start receiving
            ObjectMessage message = (ObjectMessage)consumer.receiveNoWait();
            // receive everything
            while ( message != null ) {
                logger.info("Message received");
                WarehouseData data = message.getBody(WarehouseData.class);
                out.add(data);
                String ackMessage = 
                    data.getWarehouseApplicationID() 
                    + ";"
                    + data.getWarehouseID();

                CentralManager.getSender("acknowledgements")
                    .sendTextMessageToQueue(ackMessage);
                logger.info(String.format("Acknowledged: %s", ackMessage));

                message.acknowledge();
                message = (ObjectMessage) consumer.receiveNoWait();
            }

        } catch (Exception e) {
            logger.error(e.toString());
        }

        return out;
    }

    public void stop() {
        try { consumer.close(); } catch ( Exception e ) {}
        try { session.close(); } catch ( Exception e ) {}
        try { connection.close(); } catch ( Exception e ) {}
    }
}
