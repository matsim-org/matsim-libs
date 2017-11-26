// code by jph
package playground.clruch.io.fleet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import ch.ethz.idsc.queuey.datalys.csv.CSVUtils;
import ch.ethz.idsc.queuey.util.FileDelete;
import ch.ethz.idsc.queuey.util.GlobalAssert;
import playground.clruch.dispatcher.core.RequestStatus;

public class CsvFleetReader {

    private final DayTaxiRecord dayTaxiRecord;

    public CsvFleetReader(DayTaxiRecord dayTaxiRecord) {
        this.dayTaxiRecord = dayTaxiRecord;
    }

    public DayTaxiRecord populateFrom(File file) throws Exception {
        // files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        GlobalAssert.that(file.isFile());
        int dataline = 0;
        dayTaxiRecord.lastTimeStamp = null;
        String lastAVState = null;
        final Set<List<String>> transitions = new HashSet<>();
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
                if (lastAVState != null) {
                    List<String> transition = new ArrayList<String>();
                    transition.add(list.get(3));
                    transition.add(lastAVState);
                    transition.add(StringStatusMapper.apply(list.get(3), 200, 100).tag());
                    transition.add(StringStatusMapper.apply(lastAVState, 200, 100).tag());
                    transition.add(RequestStatusParser.parseRequestStatus(StringStatusMapper.apply(list.get(3), 100, 200),
                            StringStatusMapper.apply(lastAVState, 200, 100)).tag());
                    transitions.add(transition);
                }

                lastAVState = list.get(3);

                ++dataline;
            }
        } finally {
            // ...
        }
        // }

        // Going through all timestamps and check for offservice vehicles && parse requests
        final int MAXTIME = dayTaxiRecord.getNow(dayTaxiRecord.lastTimeStamp);
        final int TIMESTEP = 10;

        // Create logs for taxiTrails
        System.out.println("INFO Checking for OFFSERVICE & RequestStatus for " + dayTaxiRecord.size() + " vehicles");
        File logFolder = new File("logs/" + file.getName().substring(0, 10));
        if (logFolder.exists())
            FileDelete.of(logFolder, 2, 10000);
        logFolder.mkdirs();
        
        for (int vehicleIndex = 0; vehicleIndex < dayTaxiRecord.size(); ++vehicleIndex) {
            String logTrailFile = (logFolder + "/Trail_" + vehicleIndex + "_Status" + ".txt");
            FileWriter fstream = new FileWriter(logTrailFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Time \t AVStatus \t RequestStatus \n");
            TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            if (vehicleIndex % 10 == 0)
                System.out.println("INFO Checking vehicle: " + vehicleIndex);

            for (int now = 0; now < MAXTIME; now += TIMESTEP) {
                taxiTrail.checkOffService(now);
            }

            // System.out.println("Found keyset for vehicle " + vehicleIndex + ": " + dayTaxiRecord.get(vehicleIndex).getKeySet().toString());
            for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                // System.out.println("Parsing requestStatus for vehicle " + vehicleIndex + " at time " + now);
                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));
                
                if (taxiTrail.interp(now).getValue().requestStatus == RequestStatus.REQUESTED)
                    if (taxiTrail.getLastEntry(now).getValue().requestStatus == RequestStatus.DRIVING)
                        taxiTrail.setRequestStatus(taxiTrail.getLastEntry(now).getKey(), RequestStatus.DROPOFF);

                // Writing each TaxiTrail to a file to check output
                out.write(now + " \t " + taxiTrail.interp(now).getValue().avStatus + " \t\t " + taxiTrail.interp(now).getValue().requestStatus + "\n");
                out.flush(); // Flush the buffer and write all changes to the disk
            }
            out.close(); // Close the file
        }
        
        String logAVTransitions = (logFolder + "/Transitions_HashMap.txt");
        FileWriter fstream = new FileWriter(logAVTransitions);
        BufferedWriter out = new BufferedWriter(fstream);
        for (List<String> transition : transitions) {
            out.write("Transition Nr: " + transition.hashCode() + "\n");
            out.write("\tCSV-File String: " + transition.get(1) + " \t-->\t " + transition.get(0) + "\n");
            out.write("\tAVStatus change: " + transition.get(3) + " \t-->\t " + transition.get(2) + "\n");
            out.write("\tRequestStatus: " + transition.get(4) + "\n\n");
        }
        out.close();

        System.out.println("INFO lines      " + dataline);
        System.out.println("INFO vehicles   " + dayTaxiRecord.size());
        System.out.println("INFO listing all occured state changes: ");
        System.out.println(transitions.toString());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}
