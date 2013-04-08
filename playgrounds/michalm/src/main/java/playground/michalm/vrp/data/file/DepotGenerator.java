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

package playground.michalm.vrp.data.file;

import java.io.*;
import java.util.Set;

import org.matsim.api.core.v01.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import pl.poznan.put.util.random.*;


public class DepotGenerator
{
    public static void generateVehicles(String networkFilename, String depotsFilename, int count,
            int t1)
        throws IOException
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        MatsimNetworkReader nr = new MatsimNetworkReader(scenario);
        nr.readFile(networkFilename);

        Set<Id> linkIdSet = scenario.getNetwork().getLinks().keySet();
        Id[] linkIds = linkIdSet.toArray(new Id[linkIdSet.size()]);
        UniformRandom uniform = RandomUtils.getGlobalUniform();

        PrintWriter pw = new PrintWriter(new File(depotsFilename));
        pw.println("<?xml version=\"1.0\" ?>");
        pw.println("<!DOCTYPE depots SYSTEM \"http://www.man.poznan.pl/~michalm/matsim/depots_v1.dtd\">");
        pw.println();

        pw.println("<depots>");

        for (int i = 0; i < count; i++) {
            pw.printf("\t<depot linkId=\"%s\">", linkIds[uniform.nextInt(0, linkIds.length - 1)]);
            pw.println();

            pw.println("\t\t<vehicles>");

            pw.printf("\t\t\t<vehicle name=\"taxi_%d\" t0=\"0\" t1=\"%d\"/>", i, t1);
            pw.println();

            pw.println("\t\t</vehicles>");
            pw.println("\t</depot>");
        }

        pw.println("</depots>");
        pw.close();
    }


    public static void main(String[] args)
        throws IOException
    {
        int count = 1000;
        int t1 = 10 * 60 * 60;

        String dir = "d:\\michalm\\poznan-via\\";
        String networkFilename = dir + "network.xml";
        String depotsFilenam = dir + "depots-taxis-" + count + ".xml";

        generateVehicles(networkFilename, depotsFilenam, count, t1);
    }
}
