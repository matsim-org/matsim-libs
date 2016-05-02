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

import playground.michalm.barcelona.demand.BarcelonaServedRequests;


public class BarcelonaTaxiGenerator
{
    private static double[] getTaxisPerHourFromRequets()
    {
        double[] reqsPerHour = { 6785, 5122, 3948, 3770, 2790, 2463, 2727, 3763, 5622, 6083, 5715,
                6052, 6142, 6427, 6607, 7219, 7398, 5541, 6209, 6425, 6496, 5682, 5064, 5505 };

        double[] reqsPerTaxi = { 2.31, 2.02, 1.95, 1.91, 1.77, 1.52, 1.41, 1.53, 1.77, 1.94, 1.77,
                1.79, 1.8, 2, 2.04, 2.06, 2.08, 1.67, 1.66, 1.81, 1.86, 1.92, 1.79, 1.88 };

        return MathArrays.ebeDivide(reqsPerHour, reqsPerTaxi);
    }


    private static double[] getTaxisPerHourFromSurvey()
    {
        double[] taxisPerHour = { 1215, 1155.8, 1008.2, 976.4, 1045.2, 1473.8, 2389.4, 4568.8,
                6491.6, 7263.8, 7259.8, 7292.8, 7166, 5519.2, 4202.2, 4730.4, 6127.6, 6488, 5922.6,
                5154, 3975.6, 2822, 1758.8, 1587.4 };
        return taxisPerHour;
    }


    private static double[] getTaxis4to5(double[] taxis0to23)
    {
        double[] taxis4to5 = new double[26];
        System.arraycopy(taxis0to23, 4, taxis4to5, 0, 20);//4-23 => 0-19
        System.arraycopy(taxis0to23, 0, taxis4to5, 20, 6);//0-5 ==> 20-25
        return taxis4to5;
    }


    public static void main(String[] args)
        throws ParseException
    {
        String dir = "d:/PP-rad/Barcelona/data/";
        String networkFile = dir + "network/barcelona_network.xml";

        boolean useSurveyData = !true;
        //avg values for each hour h, so they correspond to half past h (h:30, for each hour h)
        double[] taxis0to23 = useSurveyData ? getTaxisPerHourFromSurvey()
                : getTaxisPerHourFromRequets();
        String source = useSurveyData ? "survey" : "reqs";
        String taxisFile = dir + "taxis4to5_from_" + source + "_";

        //we start at 4:30 with vehicles, and at 5:00 with requests
        double startTime = BarcelonaServedRequests.ZERO_HOUR * 3600 - 1800;

        double minWorkTime = 4.0 * 3600;
        double maxWorkTime = 12.0 * 3600;

        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

        for (int i = 45; i <= 45; i += 2) {
            BarcelonaTaxiCreator btc = new BarcelonaTaxiCreator(scenario);
            VehicleGenerator vg = new VehicleGenerator(minWorkTime, maxWorkTime, btc);
            double scale = i / 100.;
            vg.generateVehicles(MathArrays.scale(scale, getTaxis4to5(taxis0to23)), startTime, 3600);
            new VehicleWriter(vg.getVehicles()).write(taxisFile + scale + ".xml");
        }
    }
}
