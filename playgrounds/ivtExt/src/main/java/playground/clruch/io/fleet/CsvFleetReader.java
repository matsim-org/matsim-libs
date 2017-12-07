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

    public DayTaxiRecord populateFrom(File file, boolean takeLog) throws Exception {
        // files.stream().forEach(f -> GlobalAssert.that(f.isFile()));
        GlobalAssert.that(file.isFile());
        int dataline = 0;
        dayTaxiRecord.lastTimeStamp = null;
        String lastAVState = null;

        // HashSets for logging purposes
        final HashSet<List<String>> avStateTransitions = new HashSet<>();
        final HashSet<List<String>> requestStateTransitions = new HashSet<>();
        final HashSet<List<String>> requestTrails = new HashSet<>();
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
                    List<String> avStateTransition = new ArrayList<String>();
                    avStateTransition.add(list.get(3));
                    avStateTransition.add(lastAVState);
                    avStateTransition.add(StringStatusMapper.apply(list.get(3), 200, 100).tag());
                    avStateTransition.add(StringStatusMapper.apply(lastAVState, 200, 100).tag());
                    avStateTransition.add(RequestStatusParser
                            .parseRequestStatus(StringStatusMapper.apply(list.get(3), 100, 200), StringStatusMapper.apply(lastAVState, 200, 100)).tag());
                    avStateTransitions.add(avStateTransition);
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
            TaxiTrail taxiTrail = dayTaxiRecord.get(vehicleIndex);
            if (vehicleIndex % 10 == 0)
                System.out.println("INFO Checking vehicle: " + vehicleIndex);

            for (int now = 0; now < MAXTIME; now += TIMESTEP) {
                taxiTrail.checkOffService(now);
            }

            String logTrailFile = (logFolder + "/Trail_" + vehicleIndex + "_Status" + ".txt");
            FileWriter fstream = new FileWriter(logTrailFile);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write("Time \t AVStatus \t RequestStatus \n");

            // System.out.println("Found keyset for vehicle " + vehicleIndex + ": " + dayTaxiRecord.get(vehicleIndex).getKeySet().toString());
            int iteration = 0;
            RequestContainerUtils rcUtils = new RequestContainerUtils(taxiTrail, null);
            for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                // System.out.println("Parsing requestStatus for vehicle " + vehicleIndex + " at time " + now);
                taxiTrail.setRequestStatus(now, RequestStatusParser.parseRequestStatus(now, taxiTrail));
                if (Objects.nonNull(taxiTrail.getLastEntry(now)) && RequestStatusParser.isOutlierRequest(taxiTrail.interp(now).getValue().requestStatus,
                        taxiTrail.getLastEntry(now).getValue().requestStatus))
                    taxiTrail.setRequestStatus(taxiTrail.getLastEntry(now).getKey(), RequestStatus.CANCELLED);

                if (takeLog) {
                    // Add all kind of "Vermittlungsstatus" change transitions into hash map
                    if (iteration > 2) {
                        List<String> requestStateTransition = new ArrayList<String>();
                        requestStateTransition.add(taxiTrail.interp(now).getValue().avStatus.tag());
                        requestStateTransition.add(taxiTrail.getLastEntry(now).getValue().avStatus.tag());
                        requestStateTransition.add(taxiTrail.getLastEntry(taxiTrail.getLastEntry(now).getKey()).getValue().avStatus.tag());
                        requestStateTransition.add(taxiTrail.interp(now).getValue().requestStatus.tag());
                        requestStateTransition.add(taxiTrail.getLastEntry(now).getValue().requestStatus.tag());
                        requestStateTransition.add(taxiTrail.getLastEntry(taxiTrail.getLastEntry(now).getKey()).getValue().requestStatus.tag());
                        requestStateTransitions.add(requestStateTransition);
                    }

                    // Writing each TaxiTrail to a file to check output
                    out.write(now + " \t " + taxiTrail.interp(now).getValue().avStatus + " \t\t " + taxiTrail.interp(now).getValue().requestStatus + "\n");
                    out.flush(); // Flush the buffer and write all changes to the disk
                    iteration++;
                }
            }

            if (takeLog) {
                for (Integer now : dayTaxiRecord.get(vehicleIndex).getKeySet()) {
                    if (rcUtils.isValidRequest(now, false)) {
                        List<String> requestTrail = new ArrayList<String>();
                        requestTrail = rcUtils.dumpRequestTrail(now);
                        requestTrails.add(requestTrail);
                    }
                }

                String logAVTransitions = (logFolder + "/Transitions_HashMap.txt");
                FileWriter fstream2 = new FileWriter(logAVTransitions);
                BufferedWriter out2 = new BufferedWriter(fstream2);
                out2.write("AVStatus Transition Hashmap:\n\n");
                for (List<String> avStateTransition : avStateTransitions) {
                    out2.write("Transition Nr: " + avStateTransition.hashCode() + "\n");
                    out2.write("\tCSV-File String: " + avStateTransition.get(1) + " \t-->\t " + avStateTransition.get(0) + "\n");
                    out2.write("\tAVStatus change: " + avStateTransition.get(3) + " \t-->\t " + avStateTransition.get(2) + "\n");
                    out2.write("\tRequestStatus: " + avStateTransition.get(4) + "\n\n");
                }
                out2.write("RequestStatus Transition Hashmap: (including previous and follow up status)\n\n");
                for (List<String> requestStateTransition : requestStateTransitions) {
                    out2.write("Transition Nr: " + requestStateTransition.hashCode() + "\n");
                    out2.write("\tAVStatus change: \t  " + requestStateTransition.get(2) + " \t-->\t " + requestStateTransition.get(1) + " \t-->\t "
                            + requestStateTransition.get(0) + "\n");
                    out2.write("\tRequestStatus change: " + requestStateTransition.get(5) + " \t-->\t " + requestStateTransition.get(4) + " \t-->\t "
                            + requestStateTransition.get(3) + "\n\n");

                }
                out2.write("RequestTrails Hashmap:\n\n");
                for (List<String> requestTrail : requestTrails) {
                    out2.write("\n");
                    for (int i = 0; i < requestTrail.size(); i++) {
                        out2.write(requestTrail.get(i));
                        if (i != requestTrail.size() - 1)
                            out2.write(" > ");
                    }
                }
                out2.close();
            }
            out.close(); // Close the file
        }

        System.out.println("INFO lines      " + dataline);
        System.out.println("INFO vehicles   " + dayTaxiRecord.size());
        // System.out.println("INFO listing all occured state changes: ");
        // System.out.println(avStateTransitions.toString());
        // System.out.println("timestamps " + dayTaxiRecord.keySet().size());
        System.out.println(dayTaxiRecord.status);
        return dayTaxiRecord;
    }
}
