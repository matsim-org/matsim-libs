// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;

public class CsvFleetReader {

    private final DayTaxiRecord dayTaxiRecord;

    public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
        this.dayTaxiRecord = dayTaxiRecord;
    }

    public DayTaxiRecord populateFrom(List<File> files) throws Exception {
        files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        int dataline = 0;
        dayTaxiRecord.lastTimeStamp = null;
        for (File file : files) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                {
                    String line = br.readLine();
                    List<String> list = CSVUtils.csvLineToList(line, ";");
                    int count = 0;
                    System.out.println("CSV HEADER");
                    for (String token : list) {
                        System.out.println(" col " + count + " = " + token);
                        ++count;
                    }
                }
                while (true) {
                    String line = br.readLine();
                    if (Objects.isNull(line))
                        break;
                    List<String> list = CSVUtils.csvLineToList(line, ";");
                    dayTaxiRecord.insert(list);
                    dayTaxiRecord.lastTimeStamp = list.get(0);
                    ++dataline;
                }
            } finally {
                // ...
            }
        }
        
        // Going through all timestamps and check for offservice vehicles
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;

        System.out.println("Checking for OFFSERVICE for " + dayTaxiRecord.size() + " vehicles...");
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
                // Check and propagate offservice status
                dayTaxiRecord.get(vehicleIndex).check_offservice(now);
            }
        }
        
        System.out.println("lines      " + dataline);
        System.out.println("vehicles   " + dayTaxiRecord.size());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}
