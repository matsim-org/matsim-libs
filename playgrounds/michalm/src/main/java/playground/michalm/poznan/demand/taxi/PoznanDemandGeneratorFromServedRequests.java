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

package playground.michalm.poznan.demand.taxi;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import playground.michalm.demand.taxi.AbstractDemandGeneratorFromServedRequests;


public class PoznanDemandGeneratorFromServedRequests
    extends AbstractDemandGeneratorFromServedRequests
{
    public PoznanDemandGeneratorFromServedRequests(Scenario scenario)
    {
        super(scenario);
    }


    private Map<Id<Person>, Integer> prebookingTimes = new HashMap<>();


    public void generateDemand(Iterable<PoznanServedRequest> requests, Date timeZero)
    {
        for (PoznanServedRequest r : requests) {
            int bookingTime = getTime(r.accepted, timeZero);//TODO call time??
            int startTime = getTime(r.assigned, timeZero);//TODO dispatch time??

            Person passenger = generatePassenger(r, startTime);

            if (bookingTime < startTime) {//TODO use some threshold here, e.g. 15 minutes??
                prebookingTimes.put(passenger.getId(), bookingTime);
            }
        }
    }


    private int getTime(Date time, Date timeZero)
    {
        return (int) ( (time.getTime() - timeZero.getTime()) / 1000);
    }


    public static void main(String[] args)
    {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());

        Iterable<PoznanServedRequest> requests = PoznanServedRequests.readRequests(4);
        Date zeroDate = PoznanServedRequestsReader.parseDate("09-04-2014 00:00:00");
        Date fromDate = PoznanServedRequestsReader.parseDate("09-04-2014 04:00:00");
        requests = PoznanServedRequests.filterNext24Hours(requests, fromDate);
        requests = PoznanServedRequests.filterRequestsWithinAgglomeration(requests);

        PoznanDemandGeneratorFromServedRequests dg = new PoznanDemandGeneratorFromServedRequests(
                scenario);
        dg.generateDemand(requests, zeroDate);
        dg.write("d:/PP-rad/taxi/poznan-supply/dane/zlecenia_obsluzone/plans_09_04_2014.xml");
    }
}
