/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.michalm.supply;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.contrib.util.random.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;


public class RandomVehicleGenerator
{
    public static void generateVehicles(String networkFile, String vehiclesFile, int count, int t1)
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario.getNetwork());
        nr.readFile(networkFile);

        Collection<? extends Link> linkCollection = scenario.getNetwork().getLinks().values();

        Link[] links = linkCollection.toArray(new Link[linkCollection.size()]);
        UniformRandom uniform = RandomUtils.getGlobalUniform();

        List<Vehicle> vehicles = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Id<Vehicle> id = Id.create(i + "", Vehicle.class);
            Link startLink = links[uniform.nextInt(0, links.length - 1)];
            Vehicle v = new VehicleImpl(id, startLink, 1, 0, t1);
            vehicles.add(v);
        }

        new VehicleWriter(vehicles).write(vehiclesFile);
    }


    public static void main(String[] args)
    {
        int count = 5000;
        int t1 = 30 * 60 * 60;

        String dir = "d:\\michalm\\eCab\\";
        String networkFile = dir + "2kW.15.output_network.xml";
        String vehiclesFile = dir + "taxis-" + count + ".xml";

        generateVehicles(networkFile, vehiclesFile, count, t1);
    }
}
