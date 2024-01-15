# SYT Dezentrale Systeme 7.2 Warehouse Message Oriented Middleware

## Einführung

## Apache Active MQ in Docker

Mit `docker pull rmohr/activemq` kann man die Image Datei für ActiveMQ holen.

Mit `docker run -p 61616:61616 -p 8161:8161 rmohr/activemq` kann man den Container starten, aber 
es kommt dann ein Fehler. Diese kann man fixen mit:

### Fix für Fehler

```
sudo systemctl edit docker

   [Service]
   ExecStart=
   ExecStart=/usr/bin/dockerd --default-ulimit nofile=65536:65536 -H fd://

sudo systemctl restart docker
```

### Erklärung

61616 ist der Port, auf dem der JMX broker listened, die Adminkonsole dann auf 8161.

Username und Passwort sind *admin*

## Änderungen an der Aufgabe vom ersten Modul

Da ich für die EK mehrere Warehouses brauche, habe ich den Port mit den args einstellbar gemacht:

```java
// WarehouseApplication
if(args.length > 0) {
    System.out.println("Running on Port " + args[0]);
    SpringApplication app = new SpringApplication(WarehouseApplication.class);
    app.setDefaultProperties(
        Collections.singletonMap("server.port", Integer.parseInt( args[0]))
    );
    app.run();
}
else {
    SpringApplication.run(WarehouseApplication.class);
}
```

Man muss dann das Programm mit `./gradlew bootRun --args=8081` bzw. 8082 starten.

Update: Weil gradle bei mir nicht funktioniert, bin ich auf maven umgestiegen.

`mvn spring-boot:run -Dspring-boot.run.arguments=8081` bzw. 8082 (oder irgendeinen anderen Port)


Außerdem wurden Logger hinzugefügt.

```java
// WarehouseController
public class WarehouseController {
    Logger logger = LoggerFactory.getLogger(WarehouseController.class);

public WarehouseData warehouseData( @PathVariable String inID ) {
    logger.info(String.format("called /warehouse/%s/data"), inID);
```

Um die Warehouses identifizieren zu können, wurde beim WarehouseData ein neues Attribut hinzugefügt:

```java
public class WarehouseData {
    private String warehouseApplicationID;
    // auch setter und getter


public class WarehouseApplication {
    public static String warehouseApplicationID = "warehouse8080";

        // ...
        // wenn der Port geändert wurde:
        warehouseApplicationID = "warehouse" + port;
        

public class WarehouseSimulation {
    data.setWarehouseApplicationID(WarehouseApplication.warehouseApplicationID);
```

Schließlich muss es für die EKV eine Rückmeldung von der Zentrale zu den Warehouses mit JMS geben,
es wird also ein Receiver benötigt, der einen MessageListener hat.

Dafür gibt es die Klassen `Receiver` und `ReceiverListener`.

## Allgemeiner Aufbau meines Programms

WarehouseController stellt eine REST-Schnittstelle zur Verfügung, unter welcher ein Warehouse mit einer zufälligen ID,
von einem bestimmten Port mit einem REST-Call angesprochen wird. Dieses liefert Daten zurück, die dann in die 
ActiveMQ Topic für den jeweiligen Port gesendet wird.

### Warehouses

Wenn bei einem der Warehouses Daten angefragt werden, wird die warehouseID in einem Set gespeichert.
Dort werden die Messages gespeichert, die gesendet, aber noch nicht bestätigt wurden.

Es gibt einen JMS Listener, der auf dem Topic "acknowledgements" hört. 
Dort werden von der Zentrale die Bestätigungen geschickt (mehr Infos unten).

### Zentrale

Es gibt folgende REST REST-Schnittstellen:

- **/**: Übersicht
- **/warehouse/center/data**: Alle Daten von den gespeicherten Queues rausholen und anzeigen
- **/warehouse/center/activeports**: Alle aktuellen Queues anzeigen (mehr Infos unten).
- **/warehouse/center/transfer**: Info Seite zu Transfer
- **/warehouse/center/transfer/host:port**: Hole die Daten von einem Warehouse und gib sie in eine Queue

Bei einem Transfer wird vom host:port die REST-Schnittstelle aufgerufen. Diese wird dann in die 
Queue 'host:port' gegeben, und zu den active ports (aktuelle Queues) hinzugefügt. 

Bei einem Aufruf auf .../data werden alle aktuellen Queues gelesen, und zu jeder Nachricht
eine acknowledgement Nachricht in der Form 'warehouseApplicationID;warehouseID' zum
Topic 'acknowledgements' geschickt.

## Code

Paar wichtige Code Snippets sind:

### REST

```java
@RestController
public class CenterController {

    // hier werden Daten von den Queues geholt
    @GetMapping(value="/warehouse/center/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<WarehouseData>> centerData() {
        Map<String, List<WarehouseData>> out = new HashMap<>();

        // für jede der aktuellen Queues
        for(String s : CentralManager.getPorts()) {
            // skip this topic
            if(s.equals("acknowledgements")) 
                continue;

            // erschaffe einen Receiver (code dazu unten)
            Receiver receiver = CentralManager.getReceiver(s);
    
            // hole alle messages
            out.put(s, receiver.receiveMessages());
        }

        return out;
    }

    // hier werden die Daten mit REST von den warehouses geholt
    @GetMapping(value="/warehouse/center/transfer/{warehouseAddress}")
    public String transferData(@PathVariable String warehouseAddress) {
        logger.info("transfer port " + warehouseAddress);

        // es wird eine random warehouseID genommen
        // get the data from the warehouse via rest call
        int id = rand.nextInt(0,999);
        logger.info("getting data from warehouse id " + id);
        String url = "http://" + warehouseAddress + "/warehouse/" + id + "/data";
        WarehouseData data = new RestTemplate().getForObject(url, WarehouseData.class);

        logger.info("got data from " + warehouseAddress);

        // put the data into the queue
        CentralManager.getSender(warehouseAddress).sendMessageToQueue(data);

        return "transferred data from " + warehouseAddress;
    }
```

### JMS

#### Receiver

```java

public class Receiver {

    Logger logger = LoggerFactory.getLogger(Receiver.class);

    // default anmeldedaten
    private static String user = ActiveMQConnection.DEFAULT_USER;
    private static String password = ActiveMQConnection.DEFAULT_PASSWORD;
    private static String url = ActiveMQConnection.DEFAULT_BROKER_URL;

    // connection zeugs
    private Connection connection = null;
    private Session session = null;
    private MessageConsumer consumer = null;

    private final String queue;

    public Receiver(String queue) {

        this.queue = queue;

        logger.info("Receiver started on queue: " + queue);

        Destination destination;

        try {

            // erzeuge eine neue connection
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(user, password, url);
            // wichtig: erlaube objectmessages von der WarehouseData klasse
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

                // bestätige die nachricht ()
                message.acknowledge();
                message = (ObjectMessage) consumer.receiveNoWait();
            }

        } catch (Exception e) {
            logger.error(e.toString());
        }

        return out;
    }

```

#### Sender

```java

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

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory( user, password, url );
            connectionFactory.setTrustedPackages(List.of("at.ac.tgm.student.sgao.data", "java.util"));
            connection = connectionFactory.createConnection();
            connection.start();

            // Create the session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            if(queue.equals("acknowledgements")) {
                destination = session.createTopic(this.queue);
            }
            else {
                destination = session.createQueue( this.queue );
            }

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

    public void sendTextMessageToQueue(String message) {
        try {
             TextMessage textMessage = session.createTextMessage(message);
            producer.send(textMessage);

        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    
    }
```
