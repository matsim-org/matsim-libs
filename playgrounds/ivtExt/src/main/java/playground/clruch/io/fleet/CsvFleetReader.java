// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.GlobalAssert;

public class CsvFleetReader {

    private final DayTaxiRecord dayTaxiRecord;

    public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
        this.dayTaxiRecord = dayTaxiRecord;
    }

    public DayTaxiRecord populateFrom(File file, boolean takeLog) throws Exception {
        // files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        GlobalAssert.that(file.isFile());
        int dataline = 0;
        dayTaxiRecord.lastTimeStamp = null;
        String lastAVState = null;
        File logFolder = new File("logs/" + file.getName().substring(0, 10));

        // HashSets for logging purposes
        final HashSet<List<String>> avStateTransitions = new HashSet<>();
        // for (File file : files) {

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

                // Add all kind of "Vermittlungsstatus" change transitions into hash map
                if (lastAVState != null && takeLog) {
                    avStateTransitions.add(FleetReaderLogUtils.getAVStateTransition(list.get(3), lastAVState));
                }
                lastAVState = list.get(3);
                ++dataline;
            }
        } finally {
            if (takeLog)
                FleetReaderLogUtils.writeAVTransitions(logFolder, avStateTransitions);
        }
        System.out.println("INFO lines      " + dataline);
        System.out.println("INFO vehicles   " + dayTaxiRecord.size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}