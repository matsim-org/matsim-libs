package playground.clruch.io.fleet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** @author Andreas Aumiller */
public enum FleetReaderLogUtils {
    ;

    public static List<String> getAVStateTransition(String avState, String lastAVState) {
        List<String> avStateTransition = new ArrayList<String>();
        avStateTransition.add(avState);
        avStateTransition.add(lastAVState);
        avStateTransition.add(StringStatusMapper.apply(avState, 200, 100).tag());
        avStateTransition.add(StringStatusMapper.apply(lastAVState, 200, 100).tag());
        avStateTransition.add(
                RequestStatusParser.parseRequestStatus(StringStatusMapper.apply(avState, 100, 200), StringStatusMapper.apply(lastAVState, 200, 100)).tag());
        return avStateTransition;
    }

    public static List<String> getRequestStateTransition(Integer now, TaxiTrail taxiTrail) {
        List<String> requestStateTransition = new ArrayList<String>();
        requestStateTransition.add(taxiTrail.interp(now).getValue().avStatus.tag());
        requestStateTransition.add(taxiTrail.getLastEntry(now).getValue().avStatus.tag());
        requestStateTransition.add(taxiTrail.getLastEntry(taxiTrail.getLastEntry(now).getKey()).getValue().avStatus.tag());
        requestStateTransition.add(taxiTrail.interp(now).getValue().requestStatus.tag());
        requestStateTransition.add(taxiTrail.getLastEntry(now).getValue().requestStatus.tag());
        requestStateTransition.add(taxiTrail.getLastEntry(taxiTrail.getLastEntry(now).getKey()).getValue().requestStatus.tag());
        return requestStateTransition;
    }

    public static void writeTrailLogFile(File logFolder, TaxiTrail taxiTrail, Integer vehicleIndex) throws IOException {
        String logTrailFile = (logFolder + "/Trail_" + vehicleIndex + "_Status" + ".txt");
        FileWriter fstream = new FileWriter(logTrailFile);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("Time \t AVStatus \t RequestStatus \n");

        // System.out.println("Found keyset for vehicle " + vehicleIndex + ": " + dayTaxiRecord.get(vehicleIndex).getKeySet().toString());
        for (Integer now : taxiTrail.getKeySet()) {
            out.write(now + " \t " + taxiTrail.interp(now).getValue().avStatus + " \t\t " + taxiTrail.interp(now).getValue().requestStatus + "\n");
            out.flush(); // Flush the buffer and write all changes to the disk
        }
        out.close(); // Close the file
    }
    
    public static void writeAVTransitions(File logFolder, HashSet<List<String>> avStateTransitions) throws IOException {
        String logFile = (logFolder + "/AVTransitions_HashMap.txt");
        FileWriter fstream = new FileWriter(logFile);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("AVStatus Transition Hashmap:\n\n");
        for (List<String> avStateTransition : avStateTransitions) {
            out.write("Transition Nr: " + avStateTransition.hashCode() + "\n");
            out.write("\tCSV-File String: " + avStateTransition.get(1) + " \t-->\t " + avStateTransition.get(0) + "\n");
            out.write("\tAVStatus change: " + avStateTransition.get(3) + " \t-->\t " + avStateTransition.get(2) + "\n");
            out.write("\tRequestStatus: " + avStateTransition.get(4) + "\n\n");
        }
        out.close();
    }
    
    public static void writeRequestTransitions(File logFolder, HashSet<List<String>> requestStateTransitions) throws IOException {
        String logFile = (logFolder + "/RequestTransitions_HashMap.txt");
        FileWriter fstream = new FileWriter(logFile);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("RequestStatus Transition Hashmap: (including previous and follow up status)\n\n");
        for (List<String> requestStateTransition : requestStateTransitions) {
            out.write("Transition Nr: " + requestStateTransition.hashCode() + "\n");
            out.write("\tAVStatus change: \t  " + requestStateTransition.get(2) + " \t-->\t " + requestStateTransition.get(1) + " \t-->\t "
                    + requestStateTransition.get(0) + "\n");
            out.write("\tRequestStatus change: " + requestStateTransition.get(5) + " \t-->\t " + requestStateTransition.get(4) + " \t-->\t "
                    + requestStateTransition.get(3) + "\n\n");

        }
        out.close();
    }
    
    public static void writeRequestTrails(File logFolder, HashSet<List<String>> requestTrails) throws IOException {
        String logFile = (logFolder + "/RequestTrails.txt");
        FileWriter fstream = new FileWriter(logFile);
        BufferedWriter out = new BufferedWriter(fstream);
        out.write("RequestTrails Hashmap:\n\n");
        for (List<String> requestTrail : requestTrails) {
            out.write("\n");
            for (int i = 0; i < requestTrail.size(); i++) {
                out.write(requestTrail.get(i));
                if (i != requestTrail.size() - 1)
                    out.write(" > ");
            }
        }
        out.close();
    }

}
