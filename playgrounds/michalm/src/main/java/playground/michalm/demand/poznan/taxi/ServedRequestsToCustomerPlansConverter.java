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

package playground.michalm.demand.poznan.taxi;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;

import playground.michalm.demand.poznan.taxi.PoznanServedRequestsReader.ServedRequest;
import playground.michalm.taxi.TaxiRequestCreator;


public class ServedRequestsToCustomerPlansConverter
{
    private final Scenario scenario;
    private final NetworkImpl network;
    private final PopulationFactory pf;


    public ServedRequestsToCustomerPlansConverter(Scenario scenario)
    {
        this.scenario = scenario;
        network = (NetworkImpl)scenario.getNetwork();
        pf = scenario.getPopulation().getFactory();
    }


    private int curentAgentId = 0;
    private Map<Id, Integer> prebookingTimes = new HashMap<Id, Integer>();


    public void generatePlansFor(List<ServedRequest> requests, Date timeZero)
    {
        for (ServedRequest r : requests) {
            int acceptedTime = getTime(r.accepted, timeZero);
            int assignedTime = getTime(r.assigned, timeZero);

            int pickupTime = assignedTime;//TODO simplification 

            Plan plan = pf.createPlan();

            // act0
            Activity startAct = createActivity("dummy", r.from);
            startAct.setEndTime(pickupTime);
            plan.addActivity(startAct);

            // leg
            plan.addLeg(pf.createLeg(TaxiRequestCreator.MODE));

            // act1
            plan.addActivity(createActivity("dummy", r.to));

            String strId = String.format("taxi_customer_%5d", curentAgentId++);
            Person person = pf.createPerson(scenario.createId(strId));

            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);

            if (acceptedTime < assignedTime) {//TODO use some threshold here??
                prebookingTimes.put(person.getId(), acceptedTime);
            }
        }
    }


    private Activity createActivity(String actType, Coord coord)
    {
        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
        activity.setLinkId(network.getNearestLink(coord).getId());
        return activity;
    }


    private int getTime(Date time, Date timeZero)
    {
        return (int) ( (time.getTime() - timeZero.getTime()) / 1000);
    }
}
