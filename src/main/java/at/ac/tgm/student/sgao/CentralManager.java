package at.ac.tgm.student.sgao;

import java.util.*;

/**
 * This class manages the senders.
 */
public class CentralManager {
    private class SenderReceiverPair{
        public Sender sender;
        public Receiver receiver;
        public SenderReceiverPair(Sender sender, Receiver receiver) {
            this.sender = sender;
            this.receiver = receiver;
        }
    }

    private static HashMap<String, SenderReceiverPair> senders;

    /**
     * @return a list of queues that a sender was created for
     */
    public static List<String> getPorts() {
        return List.copyOf(senders.keySet());
    }

    /**
     * Returns a sender for a specific queue or creates one if it didnt exist.
     * @param queue the queue to create a sender
     * @return the sender
     */
    public static Sender getSender(String queue) {
        if(senders.containsKey(queue)) {
            return senders.get(queue).sender;
        }
        else {
            Sender sender = new Sender(queue);
            Receiver receiver = new Receiver(queue);
            senders.put(queue, new SenderReceiverPair(sender, receiver));
            return sender;
        }
    }
}
