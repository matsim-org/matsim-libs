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
        int idcounter = 0; // TODO do more elegant with the database!
        for (File file : files) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                {
                    // String line = br.readLine();
                    String realHeader = "LATITUDE LONGITUDE OCCUPANCY TIME"; // TODO remove magic const.
                    String line = realHeader;
                    List<String> list = CSVUtils.csvLineToList(line, " ");
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
                    List<String> list = CSVUtils.csvLineToList(line, " ");
                    dayTaxiRecord.insert(list, idcounter);
                    dayTaxiRecord.lastTimeStamp = list.get(3);
                    ++dataline;
                }
            } finally {
                // ...
            }
            ++idcounter;
        }

        // // Writing each TaxiTrail to a file to check output
        // for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
        // System.out.println("Writing taxiTrail data to file of vehicle: " + vehicleIndex);
        // String logTrailFile = "logs/TaxiTrail_" + vehicleIndex + String.valueOf(".txt");
        // FileWriter fstream = new FileWriter(logTrailFile);
        // BufferedWriter out = new BufferedWriter(fstream);
        // for (Map.Entry<Integer, TaxiStamp> entry : dayTaxiRecord.get(vehicleIndex).sortedMap.entrySet()) {
        // // dayTaxiRecord.get(vehicleIndex).setRequestStatus(entry.getKey(), RequestStatusParser.parseRequestStatus(entry.getKey(),
        // dayTaxiRecord.get(vehicleIndex)));
        // out.write(entry.getKey() + "\t" + entry.getValue().requestStatus + "\n");
        // out.flush(); // Flush the buffer and write all changes to the disk
        // }
        // out.close(); // Close the file
        // }

        // Going through all timestamps and check for offservice vehicles && parse requests

        // TODO put both time we did this in one function, no double code
        final int MAXTIME = (int) Long.parseLong(dayTaxiRecord.lastTimeStamp) / 1000;

        // dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);

        // OLD final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;

        System.out.println("INFO Checking for OFFSERVICE & RequestStatus for " + dayTaxiRecord.size() + " vehicles...");
        for (int now = 0; now < MAXTIME; now += TIMESTEP) {
            if (now % 10000 == 0)
                System.out.println("now=" + now);
            for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
                TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);

                // Check and propagate offservice status
                taxiTrail.checkOffService(now);
                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));
            }
        }

        System.out.println("INFO lines      " + dataline);
        System.out.println("INFO vehicles   " + dayTaxiRecord.size());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}
