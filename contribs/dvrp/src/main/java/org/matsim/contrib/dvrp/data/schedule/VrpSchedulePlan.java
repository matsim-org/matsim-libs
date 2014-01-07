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

package org.matsim.contrib.dvrp.data.schedule;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.data.MatsimVrpData;
import org.matsim.contrib.dvrp.data.network.VrpPath;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;


public class VrpSchedulePlan
    implements Plan
{
    private final PopulationFactory populFactory;
    private final Network network;

    private final Vehicle vehicle;

    private final List<PlanElement> actsLegs;
    private final List<PlanElement> unmodifiableActsLegs;

    private Person person;


    public VrpSchedulePlan(Vehicle vehicle, MatsimVrpData data)
    {
        this.vehicle = vehicle;

        actsLegs = new ArrayList<PlanElement>();
        unmodifiableActsLegs = (List<PlanElement>)Collections.unmodifiableList(actsLegs);

        populFactory = data.getScenario().getPopulation().getFactory();
        network = data.getScenario().getNetwork();

        init();
    }


    private void init()
    {
        Link depotLink = vehicle.getDepot().getLink();

        Schedule<?> schedule = vehicle.getSchedule();

        if (schedule.getStatus().isUnplanned()) {// vehicle stays at the depot
            addActivity(depotLink, -1, "RtU");
            return;
        }

        // Depot - before schedule.getBeginTime()
        addActivity(depotLink, schedule.getBeginTime(), "RtP");

        for (Task t : schedule.getTasks()) {
            switch (t.getType()) {
                case DRIVE:
                    DriveTask dt = (DriveTask)t;
                    addLeg(dt.getPath());
                    break;

                case STAY:
                    StayTask wt = (StayTask)t;
                    addActivity(wt.getLink(), wt.getEndTime(), "S");
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        // Depot - after schedule.getEndTime()
        addActivity(depotLink, -1, "RtC");
    }


    private void addLeg(VrpPath path)
    {
        Leg leg = populFactory.createLeg(TransportMode.car);

        leg.setDepartureTime(path.getDepartureTime());

        Link[] links = path.getLinks();
        Id fromLinkId = path.getFromLink().getId();
        Id toLinkId = path.getToLink().getId();

        NetworkRoute netRoute = (NetworkRoute) ((PopulationFactoryImpl)populFactory).createRoute(
                TransportMode.car, fromLinkId, toLinkId);

        if (links.length > 1) {// means: fromLink != toLink

            // all except the first and last ones (== fromLink and toLink)
            ArrayList<Id> linkIdList = new ArrayList<Id>(links.length - 1);

            for (int i = 1; i < links.length - 1; i++) {
                linkIdList.add(links[i].getId());
            }

            netRoute.setLinkIds(fromLinkId, linkIdList, toLinkId);
            netRoute.setDistance(RouteUtils.calcDistance(netRoute, network));
        }
        else {
            netRoute.setDistance(0.0);
        }

        netRoute.setTravelTime(path.getTravelTime());
        netRoute.setTravelCost(path.getTravelCost());

        leg.setRoute(netRoute);
        leg.setDepartureTime(path.getDepartureTime());
        leg.setTravelTime(path.getTravelTime());
        ((LegImpl)leg).setArrivalTime(path.getArrivalTime());

        actsLegs.add(leg);
    }


    private void addActivity(Link link, int endTime, String type)
    {
        Activity act = new ActivityImpl(type, link.getCoord(), link.getId());

        if (endTime != -1) {
            act.setEndTime(endTime);
        }

        actsLegs.add(act);
    }


    @Override
    public List<PlanElement> getPlanElements()
    {
        return unmodifiableActsLegs;
    }


    @Override
    public boolean isSelected()
    {
        return true;
    }


    @Override
    public Double getScore()
    {
        return null;
    }


    @Override
    public Person getPerson()
    {
        return person;
    }


    @Override
    public Map<String, Object> getCustomAttributes()
    {
        return null;
    }


    @Override
    public void addLeg(Leg leg)
    {
        throw new UnsupportedOperationException("This plan is read-only");
    }


    @Override
    public void addActivity(Activity act)
    {
        throw new UnsupportedOperationException("This plan is read-only");
    }


    @Override
    public void setScore(Double score)
    {
        throw new UnsupportedOperationException("This plan is read-only");
    }


    @Override
    public void setPerson(Person person)
    {
        this.person = person;
    }
}
