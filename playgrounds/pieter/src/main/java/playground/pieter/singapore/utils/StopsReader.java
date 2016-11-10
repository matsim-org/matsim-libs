package playground.pieter.singapore.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by fouriep on 16/3/16.
 */
public class StopsReader {
    public static void main(String[] args) throws IOException {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        reader.readFile(args[0]);
        Map<String,String> stops = new HashMap<>();
        for (TransitStopFacility transitStopFacility : scenario.getTransitSchedule().getFacilities().values()) {
            stops.put(transitStopFacility.getId().toString(),transitStopFacility.getName());
        }
        BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
        for (Map.Entry<String, String> stop : stops.entrySet()) {
            writer.write("\""+stop.getKey() +"\""+ "\t" + "\""+ stop.getValue() +"\""+ "\n");
        }
        writer.close();

    }
}
