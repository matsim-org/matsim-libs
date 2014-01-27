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

import java.io.*;
import java.util.Set;

import org.matsim.api.core.v01.*;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import pl.poznan.put.util.random.*;


public class VehicleGenerator
{
    public static void generateVehicles(String networkFilename, String vehiclesFilename, int count,
            int t1)
        throws IOException
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFilename);

        Set<Id> linkIdSet = scenario.getNetwork().getLinks().keySet();
        Id[] linkIds = linkIdSet.toArray(new Id[linkIdSet.size()]);
        UniformRandom uniform = RandomUtils.getGlobalUniform();

        PrintWriter pw = new PrintWriter(new File(vehiclesFilename));
        pw.println("<?xml version=\"1.0\" ?>");
        pw.println("<!DOCTYPE vehicles SYSTEM \"http://matsim.org/files/dtd/vehicles_v1.dtd\">");
        pw.println();

        pw.println("<vehicles>");

        for (int i = 0; i < count; i++) {
            Id startLinkId = linkIds[uniform.nextInt(0, linkIds.length - 1)];
            pw.printf("\t<vehicle id=\"taxi_%d\" start_link=\"%s\" t_0=\"0\" t_1=\"%d\"/>\n", i,
                    startLinkId, t1);
        }

        pw.println("</vehicles>");
        pw.close();
    }


    public static void main(String[] args)
        throws IOException
    {
        int count = 1000;
        int t1 = 10 * 60 * 60;

        String dir = "d:\\michalm\\poznan-via\\";
        String networkFilename = dir + "network.xml";
        String vehiclesFilename = dir + "vehicle-" + count + ".xml";

        generateVehicles(networkFilename, vehiclesFilename, count, t1);
    }
}
