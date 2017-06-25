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

package playground.agarwalamit.fundamentalDiagrams;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import playground.agarwalamit.utils.FileUtils;

/**
 * Created by amit on 16/02/2017.
 */

public class RunFDDataExample {

    public static void main(String[] args) {

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.createConfig());

        List<String> mainModes = Arrays.asList("car","bike","motorbike","truck");

        // queue model parameters
        QSimConfigGroup qSimConfigGroup = scenario.getConfig().qsim();
        qSimConfigGroup.setMainModes(mainModes);
        qSimConfigGroup.setTrafficDynamics(QSimConfigGroup.TrafficDynamics.withHoles);
        qSimConfigGroup.setLinkDynamics(QSimConfigGroup.LinkDynamics.PassingQ);
        qSimConfigGroup.setUsingFastCapacityUpdate(true);

        scenario.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.warn);
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);

        Vehicles vehicles = scenario.getVehicles();
        {
            VehicleType car = VehicleUtils.getFactory().createVehicleType(Id.create("car",VehicleType.class));
            car.setPcuEquivalents(1.0);
            car.setMaximumVelocity(60.0/3.6);
            vehicles.addVehicleType(car);
        }
        {
            VehicleType bike = VehicleUtils.getFactory().createVehicleType(Id.create("bike",VehicleType.class));
            bike.setPcuEquivalents(0.25);
            bike.setMaximumVelocity(15.0/3.6);
            vehicles.addVehicleType(bike);
        }
        {
            VehicleType motorbike = VehicleUtils.getFactory().createVehicleType(Id.create("motorbike",VehicleType.class));
            motorbike.setPcuEquivalents(0.25);
            motorbike.setMaximumVelocity(60.0/3.6);
            vehicles.addVehicleType(motorbike);
        }
        {
            VehicleType truck = VehicleUtils.getFactory().createVehicleType(Id.create("truck",VehicleType.class));
            truck.setPcuEquivalents(3.0);
            truck.setMaximumVelocity(30.0/3.6);
            vehicles.addVehicleType(truck);
        }
        String myDir = FileUtils.SHARED_SVN+"/projects/mixedTraffic/triangularNetwork/run314/carMotorbikeBikeTruck/holes/laneVariation/";
        String outFolder ="/1lane/";
        scenario.getConfig().controler().setOutputDirectory(myDir+outFolder);

        // a container, used to store the link properties,
        // all sides of triangle will have these properties (identical links).

        RaceTrackLinkProperties raceTrackLinkProperties = new RaceTrackLinkProperties(1000.0, 1600.0,
                60.0/3.6, 1.0, new HashSet<>(mainModes));

//        FundamentalDiagramDataGenerator fundamentalDiagramDataGenerator = new FundamentalDiagramDataGenerator( scenario );
        FundamentalDiagramDataGenerator fundamentalDiagramDataGenerator = new FundamentalDiagramDataGenerator(raceTrackLinkProperties, scenario);
        fundamentalDiagramDataGenerator.setModalShareInPCU(new Double [] {1.0,1.0,1.0,1.0}); // equal modal split
        fundamentalDiagramDataGenerator.setReduceDataPointsByFactor(1);
        fundamentalDiagramDataGenerator.setIsWritingEventsFileForEachIteration(false);
        fundamentalDiagramDataGenerator.setIsPlottingDistribution(false);
        fundamentalDiagramDataGenerator.setIsUsingLiveOTFVis(false);
        fundamentalDiagramDataGenerator.run();
    }
}
