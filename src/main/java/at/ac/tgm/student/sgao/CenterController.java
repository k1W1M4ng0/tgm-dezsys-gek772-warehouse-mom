package at.ac.tgm.student.sgao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import at.ac.tgm.student.sgao.data.WarehouseData;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * CenterController
 */

@RestController
public class CenterController {

    Logger logger = LoggerFactory.getLogger(CenterController.class);

    public final static int PORT = 8080;

    private static Random rand = new Random();

    @GetMapping("/")
    public String centerMain() {
    	String mainPage = "This is the warehouse <b>center</b> application!<br/><br/>" +
                          "<a href='http://localhost:" + PORT+ "/warehouse/center/data'>Link for getting data out of Queues</a><br/>" +
                          "<a href='http://localhost:" + PORT+ "/warehouse/center/transfer'>Link for pulling data to save into Topics</a><br/>" +
                          "<a href='http://localhost:" + PORT+ "/warehouse/center/activeports'>Link for getting the active warehouses that were pulled from</a><br/>";
        return mainPage;
    }

    @GetMapping(value="/warehouse/center/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, List<WarehouseData>> centerData() {
        Map<String, List<WarehouseData>> out = new HashMap<>();

        for(String s : CentralManager.getPorts()) {
            // skip this topic
            if(s.equals("acknowledgements")) 
                continue;

            Receiver receiver = CentralManager.getReceiver(s);

            out.put(s, receiver.receiveMessages());
        }

        return out;
    }

    @GetMapping(value="/warehouse/center/activeports", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> activeports() {
        return CentralManager.getPorts();
    }

    @GetMapping(value="/warehouse/center/transfer")
    public String transferData() {
        String page = "Enter a ip address where the warehouse is at.<br><br>"
            + "ex.: <a href='http://localhost:" + PORT + "/warehouse/center/transfer/localhost:8082'</a>"
            + "<br> for localhost, port 8082";

        return page;
    }

    @GetMapping(value="/warehouse/center/transfer/{warehouseAddress}")
    public String transferData(@PathVariable String warehouseAddress) {
        logger.info("transfer port " + warehouseAddress);

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
    
}

