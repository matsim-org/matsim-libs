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

/**
 *
 */
package vwExamples.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.file.FleetWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author saxer
 * This is a script that drops vehicles uniformly over the network. Vehicles are only dropped on links that match with drtTag
 */
public class CreatePeopleMoverVehicles {

    public static double operationStartTime = 0.; //t0
    public static double operationEndTime = 36 * 3600.;    //t1
    public static int seats = 6;
    //static File networkfile = null;
    //static String networkfolder = networkfile.getParent();
    //static int increment = 100;
    //static String drtTag = "drt";
    //static int numberOfVehicles = 1000;

    public static void main(String[] args) {
        String networkFile = args[0];
        int numberOfVehicles = Integer.parseInt(args[1]);
        String drtTag = args[2];
        CreatePeopleMoverVehicles.run(networkFile, numberOfVehicles, drtTag);
    }

    public static void createVehicles(String networkfilePath, int numberofVehicles, String drtTag) {


        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        String networkfolder = new File(networkfilePath).getParent();
        String drtFleetFile = networkfolder + "/../fleets/fleet.xml.gz";
        List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
        Random random = MatsimRandom.getLocalInstance();
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkfilePath);
        List<Id<Link>> allLinks = new ArrayList<>();
        allLinks.addAll(scenario.getNetwork().getLinks().keySet());
        for (int i = 0; i < numberofVehicles; i++) {
            Link startLink;
            do {
                Id<Link> linkId = allLinks.get(random.nextInt(allLinks.size()));
                startLink = scenario.getNetwork().getLinks().get(linkId);
            }
            while (!startLink.getAllowedModes().contains(drtTag));
            //for multi-modal networks: Only links where cars can ride should be used.
            DvrpVehicleSpecification v = ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt" + i, Vehicle.class))
                    .startLinkId(startLink.getId())
                    .capacity(seats)
                    .serviceBeginTime(operationStartTime)
                    .serviceEndTime(operationEndTime)
                    .build();
            vehicles.add(v);

        }
        new File(new File(drtFleetFile).getParent()).mkdirs();
        new FleetWriter(vehicles.stream()).write(drtFleetFile);

    }

    public static void run(String networkfileWithServiceArea, int fleetSize, String drtTag) {
//		for (int i = 1; i <= Math.ceil(numberOfVehicles/increment) ; i++) {
//			createVehicles(networkfile, i*increment);
//		}
        createVehicles(networkfileWithServiceArea, fleetSize, drtTag);
    }

}
