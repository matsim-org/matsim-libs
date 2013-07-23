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

package playground.michalm.vrp.driver;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.*;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.misc.RouteUtils;

import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import playground.michalm.vrp.data.MatsimVrpData;
import playground.michalm.vrp.data.network.*;
import playground.michalm.vrp.data.network.shortestpath.ShortestPath;


public class VrpSchedulePlan
    implements Plan
{
    private PopulationFactory populFactory;
    private Network network;

    private Vehicle vehicle;

    private List<PlanElement> actsLegs;
    private List<PlanElement> unmodifiableActsLegs;

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
        MatsimVertex depotVertex = (MatsimVertex)vehicle.getDepot().getVertex();

        Schedule schedule = vehicle.getSchedule();

        if (schedule.getStatus().isUnplanned()) {// vehicle stays at the depot
            addActivity(depotVertex, -1, "RtU");
            return;
        }

        // Depot - before schedule.getBeginTime()
        addActivity(depotVertex, schedule.getBeginTime(), "RtP");

        for (Task t : schedule.getTasks()) {
            switch (t.getType()) {
                case DRIVE:
                    DriveTask dt = (DriveTask)t;
                    addLeg((MatsimArc)dt.getArc(), dt.getBeginTime(), dt.getEndTime());
                    break;

                case SERVE:
                    ServeTask st = (ServeTask)t;
                    addActivity((MatsimVertex)st.getAtVertex(), st.getEndTime(), ""
                            + st.getRequest().getId());
                    break;

                case WAIT:
                    WaitTask wt = (WaitTask)t;
                    addActivity((MatsimVertex)wt.getAtVertex(), wt.getEndTime(), "W");
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        // Depot - after schedule.getEndTime()
        addActivity(depotVertex, -1, "RtC");
    }


    private void addLeg(MatsimArc arc, int departureTime, int arrivalTime)
    {
        ShortestPath path = arc.getShortestPath(departureTime);

        Leg leg = populFactory.createLeg(TransportMode.car);

        leg.setDepartureTime(departureTime);

        Link fromLink = arc.getFromVertex().getLink();
        Link toLink = arc.getToVertex().getLink();

        Id[] linkIds = path.linkIds;

        NetworkRoute netRoute = (NetworkRoute) ((PopulationFactoryImpl)populFactory).createRoute(
                TransportMode.car, fromLink.getId(), toLink.getId());

        if (linkIds.length > 1) {// means: fromLink != toLink

            // all except the first and last ones (== fromLink and toLink)
            ArrayList<Id> linkIdList = new ArrayList<Id>(linkIds.length - 1);

            for (int i = 1; i < linkIds.length - 1; i++) {
                linkIdList.add(linkIds[i]);
            }

            netRoute.setLinkIds(fromLink.getId(), linkIdList, toLink.getId());
            netRoute.setDistance(RouteUtils.calcDistance(netRoute, network));
        }
        else {
            netRoute.setDistance(0.0);
        }

        int travelTime = arrivalTime - departureTime;// According to the route

        netRoute.setTravelTime(travelTime);
        netRoute.setTravelCost(path.travelCost);

        leg.setRoute(netRoute);
        leg.setDepartureTime(departureTime);
        leg.setTravelTime(travelTime);
        ((LegImpl)leg).setArrivalTime(arrivalTime);

        actsLegs.add(leg);
    }


    private void addActivity(MatsimVertex vertex, int endTime, String type)
    {
        // Activity act = populFactory.createActivityFromLinkId("service",
        // vertex.getLink().getId());
        Activity act = new ActivityImpl(type, vertex.getCoord(), vertex.getLink().getId());

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
        return true;// TODO ???
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
