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
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.extensions.taxi.TaxiUtils;
import org.matsim.contrib.dvrp.run.VrpConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;


public class ServedRequestsBasedDemandGenerator
{
    private final Scenario scenario;
    private final PopulationFactory pf;


    public ServedRequestsBasedDemandGenerator(Scenario scenario)
    {
        this.scenario = scenario;
        pf = scenario.getPopulation().getFactory();
    }


    private int curentAgentId = 0;
    private Map<Id<Person>, Integer> prebookingTimes = new HashMap<>();


    public void generatePlansFor(Iterable<ServedRequest> requests, Date timeZero)
    {
        for (ServedRequest r : requests) {
            int acceptedTime = getTime(r.accepted, timeZero);
            int assignedTime = getTime(r.assigned, timeZero);

            int pickupTime = assignedTime;//TODO simplification 

            Plan plan = pf.createPlan();

            // act0
            Activity startAct = pf.createActivityFromCoord("dummy", r.from);
            startAct.setEndTime(pickupTime);
            plan.addActivity(startAct);

            // leg
            plan.addLeg(pf.createLeg(TaxiUtils.TAXI_MODE));

            // act1
            plan.addActivity(pf.createActivityFromCoord("dummy", r.to));

            String strId = String.format("taxi_customer_%d", curentAgentId++);
            Person person = pf.createPerson(Id.createPersonId(strId));

            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);

            if (acceptedTime < assignedTime) {//TODO use some threshold here, e.g. 1 minute??
                prebookingTimes.put(person.getId(), acceptedTime);
            }
        }
    }


    private int getTime(Date time, Date timeZero)
    {
        return (int) ( (time.getTime() - timeZero.getTime()) / 1000);
    }


    public void write(String plansFile)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile);
    }


    public static void main(String[] args)
    {
        Scenario scenario = ScenarioUtils.createScenario(VrpConfigUtils.createConfig());

        Iterable<ServedRequest> requests = PoznanServedRequests.readRequests(4);
        Date zeroDate = ServedRequestsReader.parseDate("09-04-2014 00:00:00");
        Date fromDate = ServedRequestsReader.parseDate("09-04-2014 04:00:00");
        Iterable<ServedRequest> filteredRequests = PoznanServedRequests.filterNext24Hours(requests,
                fromDate);
        requests = PoznanServedRequests.filterRequestsWithinAgglomeration(requests);

        ServedRequestsBasedDemandGenerator dg = new ServedRequestsBasedDemandGenerator(scenario);
        dg.generatePlansFor(filteredRequests, zeroDate);
        dg.write("d:/PP-rad/taxi/poznan-supply/dane/zlecenia_obsluzone/plans_09_04_2014.xml");
    }
}
