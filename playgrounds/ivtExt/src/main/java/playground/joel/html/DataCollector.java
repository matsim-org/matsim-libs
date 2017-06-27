package playground.joel.html;

import playground.clruch.utils.GlobalAssert;
import playground.joel.analysis.*;

import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Joel on 27.06.2017.
 */
public class DataCollector {
    int numRequests;
    int numVehicles;

    String dispatcher;
    String user;
    String date;

    File avConfig;
    File av;

    public DataCollector(String[] args) throws Exception {
        user = System.getProperty("user.name");
        date = new SimpleDateFormat("yyyy/MM/dd - HH:mm:ss").format(new Date());

        File avConfigOld = new File(args[0]);
        File folder = avConfigOld.getParentFile();
        File avOld = new File(folder, "av.xml");

        File report = new File(folder, "output/report");
        report.mkdir();
        avConfig = new File(report, "av_config.xml");
        av = new File(report, "av.xml");

        try {
            Files.deleteIfExists(avConfig.toPath());
            Files.copy(avConfigOld.toPath(), avConfig.toPath());
            Files.deleteIfExists(av.toPath());
            Files.copy(avOld.toPath(), av.toPath());
        } catch (Exception e) {
            System.out.println("ERROR: unable to create backups!");
        }
        GlobalAssert.that(av.exists() && avConfig.exists());


    }



}
