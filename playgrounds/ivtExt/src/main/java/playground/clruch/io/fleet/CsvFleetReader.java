// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.ethz.idsc.queuey.util.GlobalAssert;

public class CsvFleetReader {

    public static List<String> csvLineToList(String line) {
        return Stream.of(line.split(";")).collect(Collectors.toList());
    }

    private final DayTaxiRecord dayTaxiRecord;

    public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
        this.dayTaxiRecord = dayTaxiRecord;
    }

    public DayTaxiRecord populateFrom(List<File> files) throws Exception {
        files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        int dataline = 0;
        for (File file : files) {

            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                {
                    String line = br.readLine();
                    List<String> list = csvLineToList(line);
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
                    List<String> list = csvLineToList(line);

                    dayTaxiRecord.insert(list);
                    ++dataline;
                }
            } finally {
                // ---
            }

        }

        System.out.println("lines      " + dataline);
        System.out.println("vehicles   " + dayTaxiRecord.size());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}
