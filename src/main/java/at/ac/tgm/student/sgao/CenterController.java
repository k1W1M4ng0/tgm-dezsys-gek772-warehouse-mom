package at.ac.tgm.student.sgao;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Random;

/**
 * CenterController
 */

@RestController
public class CenterController {

    public final static int PORT = 8080;

    private static Random rand = new Random();

    @GetMapping("/")
    public String centerMain() {
    	String mainPage = "This is the warehouse <b>center</b> application!<br/><br/>" +
                          "<a href='http://localhost:" + PORT+ "/warehouse/center/data'>Link for getting data out of Topics/001/data</a><br/>" +
                          "<a href='http://localhost:" + PORT+ "/warehouse/center/transfer'>Link for pulling data to save into Topics</a><br/>";
        return mainPage;
    }

    @GetMapping(value="/warehouse/center/data", produces = MediaType.APPLICATION_JSON_VALUE)
    public void centerData() {
        System.out.println("/warehouse/center/data");
    }

    @GetMapping(value="/warehouse/center/transfer")
    public String transferData() {
        String page = "Enter a Port where the warehouse is at.<br><br>"
            + "ex.: <a href='http://localhost:" + PORT + "/warehouse/center/transfer/8082'</a>"
            + "<br> for port 8082";

        return page;
    }

    @GetMapping(value="/warehouse/center/transfer/{port}")
    public WarehouseData transferData(@PathVariable int port) {
        System.out.println("transfer port " + port);

        String url = "http://localhost:" + port + "/warehouse/" + rand.nextInt(0, 999) + "/data";
        WarehouseData product = new RestTemplate().getForObject(url, WarehouseData.class);

        return product;
    }
    
}

