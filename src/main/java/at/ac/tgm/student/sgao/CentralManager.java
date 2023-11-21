package at.ac.tgm.student.sgao;

import java.util.*;

/**
 * This class manages the senders.
 */
public class CentralManager {
    private static HashMap<String, Sender> senders;

    /**
     * @return a list of topics that a sender was created for
     */
    public static List<String> getPorts() {
        return List.copyOf(senders.keySet());
    }

    /**
     * Returns a sender for a specific topic or creates one if it didnt exist.
     * @param topic the topic to create a sender
     * @return the sender
     */
    public static Sender getSender(String topic) {
        if(senders.containsKey(topic)) {
            return senders.get(topic);
        }
        else {
            Sender sender = new Sender(topic);
            senders.put(topic, sender);
            return sender;
        }
    }
}
