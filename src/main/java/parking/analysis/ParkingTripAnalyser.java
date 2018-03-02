/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package parking.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

/**
 * Created by amit on 02.03.18.
 */

public class ParkingTripAnalyser {

    private static final Logger LOG = Logger.getLogger(ParkingTripAnalyser.class);

    public static void main(String[] args) {
        String eventsFle = "../shared-svn/projects/vw_rufbus/projekt2/parking/output/ITERS/it.0/vw202.0.01.0.events.xml.gz";
        String configFile = "../shared-svn/projects/vw_rufbus/projekt2/parking/example_scenario/vw202.0.01/vw202.0.01.output_config.xml";
        String networkFile = "../shared-svn/projects/vw_rufbus/projekt2/parking/example_scenario/vw202.0.01/vw202.0.01.output_network.xml.gz";
        String outFile = "../shared-svn/projects/vw_rufbus/projekt2/parking/output/ITERS/it.0/parkingTripStats.txt";

        EventsManager eventsManager = EventsUtils.createEventsManager();
        ParkingTripEventHandler handler = new ParkingTripEventHandler(getNetwork(networkFile), getMainActivities(configFile));
        eventsManager.addHandler(handler);
        new MatsimEventsReader(eventsManager).readFile(eventsFle);

        writeData(handler.getPerson2ParkingTrips(),outFile);
    }

    private static void writeData(Map<Id<Person>, List<ParkingTripEventHandler.ParkingTrip>> person2ParkingTrips, String outFile) {
        try(BufferedWriter writer = IOUtils.getBufferedWriter(outFile)){
            writer.write("personId\tparkingTripIndex\tparkSearchStartLink\tparkSearchStartTime\tparkSearchEndLink" +
                    "\tparkSearchEndTime\tdistanceTravelledByCarDuringParkSearchInMeter\ttravelTimeByCarDuringParkSearch" +
                    "\tmainActivityStartLink\twalkDistanceToActivityLocation\twalkTimeFromParkToActLocation\n");
            for(Id<Person> personId : person2ParkingTrips.keySet()){
                for (int index =0; index< person2ParkingTrips.get(personId).size(); index++) {
                    ParkingTripEventHandler.ParkingTrip parkingTrip = person2ParkingTrips.get(personId).get(index);
                    if (! parkingTrip.isParkTripEnded()) {
                        LOG.warn("The park trip is not ended. This can happen only if there are stuck events.");
                    }
                    writer.write(parkingTrip.getPersonId()+"\t");
                    writer.write(parkingTrip.getTripIndex()+"\t");
                    writer.write(parkingTrip.getParkSearchStartLink()+"\t");
                    writer.write(parkingTrip.getParkSearchStartTime()+"\t");
                    writer.write(parkingTrip.getParkSearchEndLink()+"\t");
                    writer.write(parkingTrip.getParkSearchEndTime()+"\t");
                    writer.write(parkingTrip.getParkSearchDistance_car()+"\t");
                    writer.write(parkingTrip.getParkSearchTime_car()+"\t");
                    writer.write(parkingTrip.getActStartLink()+"\t");
                    writer.write(parkingTrip.getParkSearchDistance_walk()+"\t");
                    writer.write(parkingTrip.getParkSearchTime_walk()+"\t");
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e ){
            throw new RuntimeException("Data is not written. Reason "+ e);
        }
    }

    private static Set<String> getMainActivities(String configFile){
        return ConfigUtils.loadConfig(configFile)
                          .planCalcScore()
                          .getActivityParams()
                          .stream()
                          .filter(ap -> ap.isScoringThisActivityAtAll())
                          .map(ap -> ap.getActivityType())
                          .collect(Collectors.toSet());
    }

    private static Network getNetwork(String networkFile){
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile);
        return ScenarioUtils.loadScenario(config).getNetwork();
    }

}
