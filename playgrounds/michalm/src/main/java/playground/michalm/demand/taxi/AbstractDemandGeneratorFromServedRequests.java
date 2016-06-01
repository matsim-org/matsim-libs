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

package playground.michalm.demand.taxi;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.routes.GenericRouteImpl;


public abstract class AbstractDemandGeneratorFromServedRequests
{
    private final Scenario scenario;
    private final NetworkImpl network;
    private final PopulationFactory pf;


    public AbstractDemandGeneratorFromServedRequests(Scenario scenario)
    {
        this.scenario = scenario;
        pf = scenario.getPopulation().getFactory();
        network = (NetworkImpl)scenario.getNetwork();
    }
    
    
    protected Person generatePassenger(ServedRequest request, double startTime)
    {
        Plan plan = pf.createPlan();

        // start
        Activity startAct = createActivityFromCoord("start", request.getFrom());
        startAct.setEndTime(startTime);

        // end
        Activity endAct = createActivityFromCoord("end", request.getTo());

        // trip
        Leg leg = pf.createLeg(TaxiModule.TAXI_MODE);
        leg.setRoute(new GenericRouteImpl(startAct.getLinkId(), endAct.getLinkId()));

        plan.addActivity(startAct);
        plan.addLeg(leg);
        plan.addActivity(endAct);

        Person passenger = pf.createPerson(Id.createPersonId(request.getId()));
        passenger.addPlan(plan);
        scenario.getPopulation().addPerson(passenger);
        return passenger;
    }


    private Activity createActivityFromCoord(String actType, Coord coord)
    {
        ActivityImpl activity = (ActivityImpl)pf.createActivityFromCoord(actType, coord);
        Link link = network.getNearestLinkExactly(coord);
        activity.setLinkId(link.getId());
        return activity;
    }


    public void write(String plansFile)
    {
        new PopulationWriter(scenario.getPopulation(), scenario.getNetwork()).write(plansFile);
    }
}
