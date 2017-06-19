/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package playground.agarwalamit.berlin.berlinBVG09;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import playground.agarwalamit.analysis.emission.filtering.FilteredEmissionPersonEventHandler;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.MapUtils;
import playground.kai.usecases.combinedEventsReader.CombinedMatsimEventsReader;

/**
 *
 * Created by amit on 09.06.17.
 */


public class BerlinEmissionAnalyzer {
    /*
     * generated from network file: repos/shared-svn/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/network.final.xml.gz
     *                  plans file: runs-svn/berlin-bvg09/bvg.run189.10pct/ITERS/it.100/bvg.run189.10pct.100.plans.filtered.selected.xml.gz
     */
    private final String networkFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/rev554B-bvg00-0.1sample.network_withRoadTypes.xml";

    private final String vehiclesFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.emissionVehicle.xml.gz";
    private final String roadTypeMappingFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/rev554B-bvg00-0.1sample.roadTypeMapping.txt";
    private final String eventsFile = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.eventsWithNetworkModeInEvents.xml.gz";

    private final String averageFleetColdEmissionFactorsFile = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/hbefa-files/v3.1/EFA_ColdStart_vehcat_2005average.txt";
    private final String averageFleetWarmEmissionFactorsFile = FileUtils.SHARED_SVN+"/projects/detailedEval/matsim-input-files/hbefa-files/v3.1/EFA_HOT_vehcat_2005average.txt";

    private final String eventsFileWithEmissionEvents = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/bvg.run189.10pct.100.eventsWithEmissionEvents.xml.gz";

    private final String finalEmissionInfo = FileUtils.RUNS_SVN+"/berlin-bvg09/bvg.run189.10pct/emissionsRelatedFiles/emissionResults.txt";

    public static void main(String[] args) {
        BerlinEmissionAnalyzer berlinEmissionAnalyzer = new BerlinEmissionAnalyzer();
        berlinEmissionAnalyzer.createEmissionEventsFile();
        berlinEmissionAnalyzer.analyseEmissionEvents();
    }

    private void analyseEmissionEvents(){
        String transitVehicleFile = FileUtils.SHARED_SVN+"/projects/bvg_3_bln_inputdata/rev554B-bvg00-0.1sample/network/transitVehicles.final.xml.gz";
        BerlinTransitVehicleTypeIdentifier berlinTransitVehicleTypeIdentifier = new BerlinTransitVehicleTypeIdentifier(transitVehicleFile);

        //read event file and analyze emissions
        EventsManager eventsManager = EventsUtils.createEventsManager();
        FilteredEmissionPersonEventHandler emissionPersonEventHandler = new FilteredEmissionPersonEventHandler();
        eventsManager.addHandler(emissionPersonEventHandler);

        /*
         * information about transit driver and transit vehicle is required to identify the vehicle type of PT
         */
        Map<Id<Person>,Id<Vehicle>> transitDriver2TransitVehicle = new HashMap<>();
        eventsManager.addHandler(new TransitDriverStartsEventHandler() {
            @Override
            public void handleEvent(TransitDriverStartsEvent event) {
                transitDriver2TransitVehicle.put(event.getDriverId(), event.getVehicleId());
            }

            @Override
            public void reset(int iteration) {
                transitDriver2TransitVehicle.clear();
            }
        });

        CombinedMatsimEventsReader reader = new CombinedMatsimEventsReader(eventsManager);
        reader.readFile(eventsFileWithEmissionEvents);

        Map<String, Map<String, Double>> vehicleType2Emissions = new HashMap<>(); // required vehicle types only
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.carUsers_berlin.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.carUsers_brandenburger.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.carUsers_airport.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.carUsers_tourists.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.commercial.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinPersonFilter.BerlinUserGroup.freight.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinTransitVehicleTypeIdentifier.BerlinTransitEmissionVehicleType.BUS_AS_HGV.toString(),new HashMap<>());
        vehicleType2Emissions.put(BerlinTransitVehicleTypeIdentifier.BerlinTransitEmissionVehicleType.TRAINS_AS_ZERO_EMISSIONS.toString(),new HashMap<>());

        Map<Id<Vehicle>, Map<String, Double>> vehicleId2EmissionsForAllVehicles = emissionPersonEventHandler.getVehicleId2TotalEmissions();
        BerlinPersonFilter berlinPersonFilter = new BerlinPersonFilter();

        for(Id<Vehicle> vehicleId : vehicleId2EmissionsForAllVehicles.keySet()) { // all vehicles
            Id<Person> personId = Id.createPersonId(vehicleId);

            // check if transit driver
            if (transitDriver2TransitVehicle.containsKey(personId)) {
                Id<Vehicle> transitVehicle = transitDriver2TransitVehicle.get(personId);
                String userGroup = berlinTransitVehicleTypeIdentifier.getBerlinTransitEmissionVehicleType(transitVehicle).toString();
                Map<String, Double> emissionsSoFar =  vehicleType2Emissions.get(userGroup);
                Map<String, Double> totalEmissions = MapUtils.addMaps(emissionsSoFar, vehicleId2EmissionsForAllVehicles.get(vehicleId));
                vehicleType2Emissions.put(userGroup, totalEmissions);
            } else {
                String userGroup = berlinPersonFilter.getUserGroupAsStringFromPersonId(personId);
                if (vehicleType2Emissions.containsKey(userGroup)) {
                    Map<String, Double> emissionsSoFar =  vehicleType2Emissions.get(userGroup);
                    Map<String, Double> totalEmissions = MapUtils.addMaps(emissionsSoFar, vehicleId2EmissionsForAllVehicles.get(vehicleId));
                    vehicleType2Emissions.put(userGroup, totalEmissions);
                } else {
                    //skip other user groups
                }
            }
        }

        writeData(vehicleType2Emissions);
    }

    private void createEmissionEventsFile(){
        Config config = ConfigUtils.createConfig();
        config.network().setInputFile(networkFile); // need to find network file
        config.vehicles().setVehiclesFile(vehiclesFile); // need to create vehicles file

        EmissionsConfigGroup emissionsConfigGroup  = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        emissionsConfigGroup.setUsingDetailedEmissionCalculation(false);
        emissionsConfigGroup.setEmissionRoadTypeMappingFile(roadTypeMappingFile);
        emissionsConfigGroup.setAverageColdEmissionFactorsFile(averageFleetColdEmissionFactorsFile);
        emissionsConfigGroup.setAverageWarmEmissionFactorsFile(averageFleetWarmEmissionFactorsFile);
        emissionsConfigGroup.setUsingVehicleTypeIdAsVehicleDescription(false);

        Scenario scenario = ScenarioUtils.loadScenario(config);

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionModule emissionModule = new EmissionModule(scenario,eventsManager);

        EventWriterXML emissionEventWriter = new EventWriterXML(eventsFileWithEmissionEvents);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);

        emissionEventWriter.closeFile();

        emissionModule.writeEmissionInformation();
    }

    private void writeData(Map<String, Map<String, Double>> vehicleType2Emissions){
        BufferedWriter writer = IOUtils.getBufferedWriter(finalEmissionInfo);
        try {
            writer.write("userGroup\tpollutant\tamountInGm\n");

            for(String ug : vehicleType2Emissions.keySet()){
                for(String pollutant : vehicleType2Emissions.get(ug).keySet()) {
                    writer.write(ug+"\t"+pollutant+"\t"+vehicleType2Emissions.get(ug).get(pollutant));
                    writer.newLine();
                }
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }
    }
}
