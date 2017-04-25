/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.agarwalamit.mixedTraffic.patnaIndia.policies.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.types.HbefaVehicleCategory;
import org.matsim.contrib.emissions.utils.EmissionSpecificationMarker;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import playground.agarwalamit.utils.FileUtils;
import playground.agarwalamit.utils.LoadMyScenarios;

/**
 * Created by amit on 23/12/2016.
 */

public class PatnaEmissionsWriter {

    private final String avgColdEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_ColdStart_vehcat_2005average.txt";
    private final String avgWarmEmissFile = FileUtils.SHARED_SVN+"projects/detailedEval/matsim-input-files/hbefa-files/v3.2/EFA_HOT_vehcat_2005average.txt";

    private final String roadTypeMappingFile = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/input/roadTypeMappingFile.txt";
    private final String networkWithRoadTypeMapping = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/input/networkWithRoadTypeMapping.xml.gz";

    public static void main(String[] args) {
        String dir = FileUtils.RUNS_SVN+"patnaIndia/run108/jointDemand/policies/0.15pcu/bau/";
        PatnaEmissionsWriter pew = new PatnaEmissionsWriter();
        pew.writeRoadTypeMappingFile(dir+"/output_network.xml.gz");
        pew.writeEmissionEventsFile(dir);
    }

    private void writeRoadTypeMappingFile(final String networkFile) {
        Scenario scenario = LoadMyScenarios.loadScenarioFromNetwork(networkFile);
        BufferedWriter writer = IOUtils.getBufferedWriter(roadTypeMappingFile);
        try {
            writer.write("VISUM_RT_NR" + ";" + "VISUM_RT_NAME" + ";"
                    + "HBEFA_RT_NAME" + "\n");
            writer.write("01" + ";" + "30kmh" + ";" + "URB/Access/30" + "\n");
            writer.write("02" + ";" + "40kmh" + ";" + "URB/Access/40"+ "\n");
            writer.write("031" + ";" + "50kmh-1l" + ";" + "URB/Local/50"+ "\n");
            writer.write("032" + ";" + "50kmh-2l" + ";" + "URB/Distr/50"+ "\n");
            writer.write("033" + ";" + "50kmh-3+l" + ";" + "URB/Trunk-City/50"+ "\n");
            writer.write("041" + ";" + "60kmh-1l" + ";" + "URB/Local/60"+ "\n");
            writer.write("042" + ";" + "60kmh-2l" + ";" + "URB/Trunk-City/60"+ "\n");
            writer.write("043" + ";" + "60kmh-3+l" + ";" + "URB/MW-City/60"+ "\n");
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException("Data is not written/read. Reason : " + e);
        }

        for (Link l : scenario.getNetwork().getLinks().values() ) {
            double freeSpeed = l.getFreespeed() * 3.6;
            int numberOfLanes = (int) l.getNumberOfLanes();
            if ( Math.round(freeSpeed) >= 60.0 )  {
                if ( numberOfLanes >=3) {
                    NetworkUtils.setType(l, "043");
                } else if (numberOfLanes >=2 ) {
                    NetworkUtils.setType(l, "042");
                } else {
                    NetworkUtils.setType(l, "041");
                }
            } else if ( Math.round(freeSpeed) >= 50.0 ) {  //
                if ( numberOfLanes >=3) {
                    NetworkUtils.setType(l, "033");
                } else if (numberOfLanes >=2 ) {
                    NetworkUtils.setType(l, "032");
                } else {
                    NetworkUtils.setType(l, "031");
                }
            } else if ( Math.round(freeSpeed) >= 40.0 ) {
                NetworkUtils.setType(l, "02");
            } else {
                NetworkUtils.setType(l, "01");
            }
        }
        new NetworkWriter(scenario.getNetwork()).write(networkWithRoadTypeMapping);
    }

    private void writeEmissionEventsFile(final String outputDir){

        EmissionsConfigGroup ecg = new EmissionsConfigGroup();
        ecg.setUsingDetailedEmissionCalculation(false);
        ecg.setUsingVehicleTypeIdAsVehicleDescription(false);
        ecg.setAverageColdEmissionFactorsFile(avgColdEmissFile);
        ecg.setAverageWarmEmissionFactorsFile(avgWarmEmissFile);
        ecg.setEmissionRoadTypeMappingFile(roadTypeMappingFile);

        Config config = ConfigUtils.loadConfig(outputDir+"/output_config.xml.gz", ecg);
        config.plans().setInputFile(null);
        config.plans().setInputPersonAttributeFile(null);

        config.network().setInputFile(networkWithRoadTypeMapping);
        config.vehicles().setVehiclesFile("output_vehicles.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);
        // need to store the vehicle description and also generate vehicles.
        for (VehicleType vt : scenario.getVehicles().getVehicleTypes().values()) {
            HbefaVehicleCategory vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            if (vt.getId().toString().equals(TransportMode.car)) vehicleCategory = HbefaVehicleCategory.PASSENGER_CAR;
            else if (vt.getId().toString().equals(TransportMode.bike)) vehicleCategory = HbefaVehicleCategory.ZERO_EMISSION_VEHICLE;
            else if  (vt.getId().toString().equals("motorbike")) vehicleCategory = HbefaVehicleCategory.MOTORCYCLE;
            else if  (vt.getId().toString().equals("truck")) vehicleCategory = HbefaVehicleCategory.HEAVY_GOODS_VEHICLE;
            else throw new RuntimeException("not implemented yet.");

            vt.setDescription(  EmissionSpecificationMarker.BEGIN_EMISSIONS.toString()+
                    vehicleCategory.toString().concat(";;;")+
                    EmissionSpecificationMarker.END_EMISSIONS.toString() );
        }

        PatnaEmissionVehicleCreator emissionVehicleCreatorHandler = new PatnaEmissionVehicleCreator(scenario);

        String emissionEventOutputFile = outputDir + "/output_emissions_events.xml.gz";

        EventsManager eventsManager = EventsUtils.createEventsManager();
        EmissionModule emissionModule = new EmissionModule(scenario, eventsManager);
        eventsManager.addHandler(emissionVehicleCreatorHandler);

        EventWriterXML emissionEventWriter = new EventWriterXML(emissionEventOutputFile);
        emissionModule.getEmissionEventsManager().addHandler(emissionEventWriter);

        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(outputDir+ "/output_events.xml.gz");

        emissionEventWriter.closeFile();

        emissionModule.writeEmissionInformation();
    }

    class PatnaEmissionVehicleCreator implements PersonDepartureEventHandler {

        private final Scenario scenario;

        PatnaEmissionVehicleCreator (final Scenario scenario) {
            this.scenario = scenario;
        }

        // cant use following, because, vehicle is required before the compiler jumps here. Amit Dec 16
//        @Override
//        public void handleEvent(VehicleEntersTrafficEvent event) {
//            VehicleType vt = scenario.getVehicles().getVehicleTypes().get(Id.create(event.getNetworkMode(), VehicleType.class));
//            Id<Vehicle> vehicleId = event.getVehicleId();
//            if (! scenario.getVehicles().getVehicles().containsKey(vehicleId)) {
//                Vehicle vehicle = scenario.getVehicles().getFactory().createVehicle(vehicleId, vt);
//                scenario.getVehicles().addVehicle(vehicle);
//            }
//        }

        @Override
        public void reset(int iteration) {

        }

        @Override
        public void handleEvent(PersonDepartureEvent event) {
            if (event.getLegMode().equals(TransportMode.pt) || event.getLegMode().equals(TransportMode.walk)) return;

            VehicleType vt = scenario.getVehicles().getVehicleTypes().get(Id.create(event.getLegMode(), VehicleType.class));
            Id<Vehicle> vehicleId;
            //following is internal thing in population agent source; probbaly, there should be a better way out.
            if(event.getLegMode().equals(TransportMode.car)) {
                vehicleId = Id.createVehicleId(event.getPersonId());
            } else {
                vehicleId = Id.createVehicleId(event.getPersonId()+"_"+event.getLegMode());
            }
            if (! scenario.getVehicles().getVehicles().containsKey(vehicleId)) {
                Vehicle vehicle = VehicleUtils.getFactory().createVehicle(vehicleId, vt);
                scenario.getVehicles().addVehicle(vehicle);
            }
        }
    }
}
