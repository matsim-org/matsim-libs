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

package playground.michalm.barcelona.supply;

import java.text.ParseException;

import org.apache.commons.math3.util.MathArrays;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.dvrp.data.VehicleGenerator;
import org.matsim.contrib.dvrp.data.file.VehicleWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.barcelona.demand.*;


public class BarcelonaTaxiGenerator
{
    public static void main(String[] args)
        throws ParseException
    {
        //25 values: 5am to 5am
        double[] reqsPerHour = { 2463, 2727, 3763, 5622, 6083, 5715, 6052, 6142, 6427, 6607, 7219,
                7398, 5541, 6209, 6425, 6496, 5682, 5064, 5505, 6785, 5122, 3948, 3770, 2790,
                2463 };
        double vehsToReqsRatio = 0.2;
        double[] vehsPerHour = MathArrays.scale(vehsToReqsRatio, reqsPerHour);

        double minWorkTime = 4.0 * 3600;
        double maxWorkTime = 12.0 * 3600;

        String dir = "d:/PP-rad/Barcelona/";
        String networkFile = dir + "barcelona_network.xml";
        String taxisFile = dir + "taxis5to5_" + vehsToReqsRatio + ".xml";

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario).readFile(networkFile);

        BarcelonaTaxiCreator btc = new BarcelonaTaxiCreator(scenario);
        VehicleGenerator vg = new VehicleGenerator(minWorkTime, maxWorkTime, btc);
        vg.generateVehicles(vehsPerHour, BarcelonaServedRequests.ZERO_HOUR * 3600, 3600);
        new VehicleWriter(vg.getVehicles()).write(taxisFile);
    }
}