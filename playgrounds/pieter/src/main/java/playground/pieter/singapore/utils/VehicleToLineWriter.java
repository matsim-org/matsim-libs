package playground.pieter.singapore.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleReaderV1;
import org.matsim.vehicles.VehicleType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by fouriep on 28/7/16.
 */
public class VehicleToLineWriter {

        public static void main(String[] args) throws IOException {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new TransitScheduleReader(scenario).readFile(args[0]);
            new VehicleReaderV1(scenario.getTransitVehicles()).readFile(args[1]);

            Map<String,Set<String>> lineToVehicleMap = new HashMap<>();
            for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
                HashSet<String> vehTypes = new HashSet<>();
                lineToVehicleMap.put(transitLine.getName(), vehTypes);
                for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                    for (Departure departure : transitRoute.getDepartures().values()) {
                        Id<Vehicle> vehicleId = departure.getVehicleId();
                        vehTypes.add(scenario.getTransitVehicles().getVehicles().get(vehicleId).getType().getDescription());
                    }

                }

            }

            BufferedWriter writer = IOUtils.getBufferedWriter(args[2]);
                    writer.write("line\tvehtype\n");
            for (Map.Entry<String, Set<String>> setEntry : lineToVehicleMap.entrySet()) {
                for (String s : setEntry.getValue()) {
                    writer.write(setEntry.getKey() +  "\t"  + setEntry.getValue()  + "\n");
                }

            }
            writer.close();

        }


}
