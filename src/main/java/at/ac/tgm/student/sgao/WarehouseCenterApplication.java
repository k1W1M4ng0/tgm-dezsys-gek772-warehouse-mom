package at.ac.tgm.student.sgao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collections;

@SpringBootApplication
public class WarehouseCenterApplication {

	public static void main(String[] args) {
		SpringApplication.run(WarehouseCenterApplication.class, args);
	}
}


// @SpringBootApplication
// public class Warehouse2Application {
//
//     public static Sender sender;
//     public static Receiver receiver;
//
//     public static boolean main = false;
//
//     public static void main(String[] args) throws InterruptedException {
//         SpringApplication app = new SpringApplication(Warehouse2Application.class);
//         app.setDefaultProperties(Collections.singletonMap("server.port", Integer.valueOf(args[1])));
//         app.run(args);
//
//         String topic = args[0];
//
//         if(topic.equals("main")) {
//             main = true;
//
//             receiver = new Receiver("war1", true);
//             sender = new Sender("war1");
//         }else {
//             receiver = new Receiver(topic, false);
//             sender = new Sender(topic);
//         }
//     }
// }
