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

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.contrib.util.random.*;
import org.matsim.core.network.*;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.common.collect.Iterables;

import playground.michalm.demand.taxi.ServedRequests;


public class BarcelonaServedRequestsBasedDemandGenerator
{
    private final Scenario scenario;
    private final NetworkImpl network;
    private final PopulationFactory pf;

    private final static UniformRandom uniform = RandomUtils.getGlobalUniform();


    public BarcelonaServedRequestsBasedDemandGenerator(Scenario scenario)
    {
        this.scenario = scenario;
        pf = scenario.getPopulation().getFactory();
        network = (NetworkImpl)scenario.getNetwork();
    }


    private int currentAgentId = 0;


    public void generatePlansFor(Iterable<BarcelonaServedRequest> requests,
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

            int pickupTime = calcStartTime(r);
            Plan plan = pf.createPlan();

            // act0
            Activity startAct = createActivityFromCoord("orig", r.from);
            startAct.setEndTime(pickupTime);
            plan.addActivity(startAct);

            // leg
            plan.addLeg(pf.createLeg(TaxiUtils.TAXI_MODE));

            // act1
            plan.addActivity(createActivityFromCoord("dest", r.to));

            String strId = String.format("taxi_customer_%d", currentAgentId++);
            Person person = pf.createPerson(Id.createPersonId(strId));

            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
        }
    }


    private Activity createActivityFromCoord(String actType, Coord coord)
    {
        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
        Link link = network.getNearestLinkExactly(coord);
        activity.setLinkId(link.getId());
        return activity;
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


    public void write(String plansFile)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile);
    }


    public static void main(String[] args)
    {
        String dir = "d:/PP-rad/Barcelona/";
        String networkFile = dir + "barcelona_network.xml";

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
            Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());
            new MatsimNetworkReader(scenario).readFile(networkFile);

            BarcelonaServedRequestsBasedDemandGenerator dg = new BarcelonaServedRequestsBasedDemandGenerator(
                    scenario);
            double scale = i / 10.;
            dg.generatePlansFor(requests, scale);
            dg.write(dir + "plans5to4_" + scale + ".xml.gz");
        }

        //new BarcelonaServedRequestsWriter(requests).writeFile("d:/PP-rad/Barcelona/served_requests/tripsInAgglomeration_since_Mar_2011_only_weekdays.csv");
    }
}
