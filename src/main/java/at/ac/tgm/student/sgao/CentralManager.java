package at.ac.tgm.student.sgao;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the senders.
 */
public class CentralManager {

    private static class SenderReceiverPair{
        public Sender sender;
        public Receiver receiver;
        public SenderReceiverPair(Sender sender, Receiver receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }

    private static HashMap<String, SenderReceiverPair> senders = new HashMap<>();
    private static Logger logger = LoggerFactory.getLogger(CentralManager.class);

    /**
     * @return a list of queues that a sender was created for
     */
    public static List<String> getPorts() {
        logger.debug("getting all ports");
        return List.copyOf(senders.keySet());
    }

    /**
     * Returns a sender for a specific queue or creates one if it didnt exist.
     * @param queue the queue to create a sender
     * @return the sender
     */
    public static Sender getSender(String queue) {
        if(senders.containsKey(queue)) {
            logger.info("returning existing sender for " + queue);
            return senders.get(queue).sender;
        }
        else {
            logger.info("creating sender and receiver for " + queue);
            Sender sender = new Sender(queue);
            Receiver receiver = new Receiver(queue);
            senders.put(queue, new SenderReceiverPair(sender, receiver));
            return sender;
        }
    }

    public static Receiver getReceiver(String queue) {
        if(senders.containsKey(queue)) {
            logger.info("returning existing receiver for " + queue);
            return senders.get(queue).receiver;
        }
        else {
            logger.info("creating sender and receiver for " + queue);
            Sender sender = new Sender(queue);
            Receiver receiver = new Receiver(queue);
            senders.put(queue, new SenderReceiverPair(sender, receiver));
            return receiver;
        }
    }
}
