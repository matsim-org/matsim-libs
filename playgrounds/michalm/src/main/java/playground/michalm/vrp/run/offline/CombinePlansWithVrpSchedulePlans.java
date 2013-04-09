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

package playground.michalm.vrp.run.offline;

import java.io.IOException;
import java.util.Arrays;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.vrp.run.VrpConfigUtils;


public class CombinePlansWithVrpSchedulePlans
{
    public static void main(String... args)
        throws IOException
    {
        String dirName;
        String networkFileName;
        String plansFileName;
        String vrpDirName;

        if (args.length == 1 && args[0].equals("test")) {// for testing
            dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec1\\";
            networkFileName = dirName + "network.xml";
            plansFileName = dirName + "output\\config-verB\\output_plans.xml.gz";
            vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\burkat_andrzej\\siec2\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\Paj\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";

            // dirName = "D:\\PP-dyplomy\\2010_11-mgr\\gintrowicz_marcin\\NSE\\";
            // networkFileName = dirName + "network.xml";
            // plansFileName = dirName + "\\output\\config-verB\\output_plans.xml.gz";
            // vrpDirName = dirName + "dvrp\\";
        }
        else if (args.length == 4) {
            dirName = args[0];
            networkFileName = dirName + args[1];
            plansFileName = dirName + args[2];
            vrpDirName = dirName + args[3];
        }
        else {
            throw new IllegalArgumentException("Incorrect program arguments: "
                    + Arrays.toString(args));
        }

        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        new MatsimNetworkReader(scenario).readFile(networkFileName);

        new MatsimPopulationReader(scenario).readFile(plansFileName);

        String vrpOutDirName = vrpDirName + "\\output";
        new MatsimPopulationReader(scenario).readFile(vrpOutDirName + "\\vrpDriverPlans.xml");

        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).writeV4(dirName
                + "\\combinedPlans.xml");
    }
}
