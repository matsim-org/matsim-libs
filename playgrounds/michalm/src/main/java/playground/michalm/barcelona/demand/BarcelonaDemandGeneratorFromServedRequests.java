/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.michalm.barcelona.demand;

import java.util.Date;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.util.random.*;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.Iterables;

import playground.michalm.demand.taxi.*;


public class BarcelonaDemandGeneratorFromServedRequests
    extends AbstractDemandGeneratorFromServedRequests
{
    private final static UniformRandom uniform = RandomUtils.getGlobalUniform();


    public BarcelonaDemandGeneratorFromServedRequests(Scenario scenario)
    {
        super(scenario);
    }


    public void generateDemand(Iterable<BarcelonaServedRequest> requests,
            double selectionProbability)
    {
        for (BarcelonaServedRequest r : requests) {
            if (!uniform.trueOrFalse(selectionProbability)) {
                continue;
            }

            //skip last hour; we want to have demand from 5am to 4am (we have taxis from 5 to 5)
            if (r.getStartTime().getHours() == BarcelonaServedRequests.ZERO_HOUR - 1) {
                continue;
            }

            generatePassenger(r, calcStartTime(r));
        }
    }


    private int calcStartTime(BarcelonaServedRequest request)
    {
        Date startTime = request.getStartTime();
        int h = startTime.getHours();
        int m = startTime.getMinutes();

        if (h < BarcelonaServedRequests.ZERO_HOUR) {
            h += 24;
        }

        return h * 3600 + m * 60 + uniform.nextInt(0, 59);
    }


    public static void main(String[] args)
    {
        String dir = "d:/PP-rad/Barcelona/data/";
        String networkFile = dir + "network/barcelona_network.xml";

        Iterable<BarcelonaServedRequest> requests = BarcelonaServedRequests.readRequests();
        System.out.println("#All: " + Iterables.size(requests));
        requests = BarcelonaServedRequests.filterFromMar2011(requests);
        System.out.println("#from Mar 2011: " + Iterables.size(requests));
        requests = ServedRequests.filterWorkDaysPeriods(requests,
                BarcelonaServedRequests.ZERO_HOUR);
        System.out.println("#on weekdays: " + Iterables.size(requests));
        requests = BarcelonaServedRequests.filterRequestsWithinAgglomeration(requests);
        System.out.println("#within BCN: " + Iterables.size(requests));

        for (int i = 2; i <= 10; i++) {
            Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
            new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFile);

            BarcelonaDemandGeneratorFromServedRequests dg = new BarcelonaDemandGeneratorFromServedRequests(
                    scenario);
            double scale = i / 10.;
            dg.generateDemand(requests, scale);
            dg.write(dir + "plans/plans5to4_" + scale + ".xml.gz");
        }

        //new BarcelonaServedRequestsWriter(requests).writeFile("d:/PP-rad/Barcelona/data/served_requests/tripsInAgglomeration_since_Mar_2011_only_weekdays.csv");
    }
}
