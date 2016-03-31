/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.*;
import org.matsim.contrib.dvrp.data.file.VehicleReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;


public class RunVehicleCount
{
    public static void main(String[] args)
        throws IOException
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        VrpData data = new VrpDataImpl();
        VehicleReader reader = new VehicleReader(scenario.getNetwork(), data);
        reader.parse(
                "d:/svn-vsp/sustainability-w-michal-and-dlr/data/scenarios/2015_02_strike/taxis.xml0.0.xml");

        VehicleCounter counter = new VehicleCounter(data.getVehicles().values());
        List<Integer> counts = counter.countVehiclesOverTime(5 * 60);

        File file = new File(
                "d:/svn-vsp/sustainability-w-michal-and-dlr/data/scenarios/2015_02_strike/taxis.xml0.0-counts.txt");
        BufferedWriter bf = new BufferedWriter(new FileWriter(file));

        int time = 0;
        bf.write("Time\tCount\n");
        for (Integer i : counts) {
            bf.write(time + "\t" + i + "\n");
            time += 5 * 60;
        }

        bf.close();
    }
}
