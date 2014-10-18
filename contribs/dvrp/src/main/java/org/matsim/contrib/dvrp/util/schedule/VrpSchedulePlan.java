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

package org.matsim.contrib.dvrp.util.schedule;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.*;


public class VrpSchedulePlan
    implements Plan
{
    private final PopulationFactory populFactory;
    private final Network network;

    private final Vehicle vehicle;

    private final List<PlanElement> actsLegs;
    private final List<PlanElement> unmodifiableActsLegs;

    private Person person;


    public VrpSchedulePlan(Vehicle vehicle, Scenario scenario)
    {
        this.vehicle = vehicle;

        actsLegs = new ArrayList<>();
        unmodifiableActsLegs = Collections.unmodifiableList(actsLegs);

        populFactory = scenario.getPopulation().getFactory();
        network = scenario.getNetwork();

        init();
    }


    private void init()
    {
        Link startLink = vehicle.getStartLink();

        Schedule<?> schedule = vehicle.getSchedule();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {// vehicle stays on startLink
            addActivity(startLink, -1, "RtU");
            return;
        }

        // before schedule.getBeginTime()
        addActivity(startLink, schedule.getBeginTime(), "RtP");

        for (Task t : schedule.getTasks()) {
            switch (t.getType()) {
                case DRIVE:
                    addLeg((DriveTask)t);
                    break;

                case STAY:
                    StayTask wt = (StayTask)t;
                    addActivity(wt.getLink(), wt.getEndTime(), "S");
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        // after schedule.getEndTime()
        addActivity(startLink, -1, "RtC");
    }


    private void addLeg(DriveTask task)
    {
        VrpPath path = task.getPath();
        Leg leg = populFactory.createLeg(TransportMode.car);

        Id<Link> fromLinkId = path.getFromLink().getId();
        Id<Link> toLinkId = path.getToLink().getId();

        NetworkRoute netRoute = (NetworkRoute) ((PopulationFactoryImpl)populFactory).createRoute(
                TransportMode.car, fromLinkId, toLinkId);

        int length = path.getLinkCount();
        if (length > 1) {// means: fromLink != toLink

            // all except the first and last ones (== fromLink and toLink)
            ArrayList<Id<Link>> linkIdList = new ArrayList<>(length - 1);

            for (int i = 1; i < length - 1; i++) {
                linkIdList.add(path.getLink(i).getId());
            }

            netRoute.setLinkIds(fromLinkId, linkIdList, toLinkId);
            netRoute.setDistance(RouteUtils.calcDistance(netRoute, network));
        }
        else {
            netRoute.setDistance(0.0);
        }

        double tt = task.getEndTime() - task.getBeginTime();

        netRoute.setTravelTime(tt);

        if (path instanceof VrpPathWithTravelData) {
            netRoute.setTravelCost( ((VrpPathWithTravelData)path).getTravelCost());
        }
        else {
            netRoute.setTravelCost(Double.NaN);
        }

        leg.setRoute(netRoute);
        leg.setDepartureTime(task.getBeginTime());
        leg.setTravelTime(tt);
        ((LegImpl)leg).setArrivalTime(task.getEndTime());

        actsLegs.add(leg);
    }


    private void addActivity(Link link, double endTime, String type)
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
    public String getType()
    {
        return "VrpSchedulePlan for vehicle " + vehicle.getId();
    }


    @Override
    public void setType(String type)
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
