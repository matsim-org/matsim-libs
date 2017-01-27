/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.util.otfvis;

import java.util.*;

import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.path.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.schedule.Schedule.ScheduleStatus;
import org.matsim.contrib.dvrp.schedule.Task.TaskType;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vis.otfvis.OnTheFlyServer.NonPlanAgentQueryHelper;


public class VrpAgentQueryHelper
    implements NonPlanAgentQueryHelper
{
    private static final double SIM_BEGIN_TIME = 0;

    private final PopulationFactory populFactory;


    public VrpAgentQueryHelper(PopulationFactory populFactory)
    {
        this.populFactory = populFactory;
    }


    @Override
    public Plan getPlan(MobsimAgent mobsimAgent)
    {
        return new VrpSchedulePlan(getVehicle(mobsimAgent));
    }


    @Override
    public Activity getCurrentActivity(MobsimAgent mobsimAgent)
    {
        Vehicle vehicle = getVehicle(mobsimAgent);
        Schedule<?> schedule = vehicle.getSchedule();

        switch (schedule.getStatus()) {
            case UNPLANNED:
                return createActivity("Unplanned", vehicle.getStartLink().getId(), SIM_BEGIN_TIME,
                        vehicle.getT1());

            case PLANNED:
                return createActivity("Before schedule", vehicle.getStartLink().getId(),
                        SIM_BEGIN_TIME, schedule.getBeginTime());

            case STARTED:
                Task currentTask = schedule.getCurrentTask();
                if (currentTask.getType() == TaskType.STAY) {
                    StayTask st = (StayTask)currentTask;
                    return createActivity(st.getType().name(), st.getLink().getId(),
                            st.getBeginTime(), st.getEndTime());
                }
                return null;

            case COMPLETED: //at that time, should be removed from simulation
            default:
        }

        throw new IllegalStateException();
    }


    private Vehicle getVehicle(MobsimAgent mobsimAgent)
    {
        return ((VrpAgentLogic) ((DynAgent)mobsimAgent).getAgentLogic()).getVehicle();
    }


    private List<PlanElement> initPlanElements(Vehicle vehicle)
    {
        List<PlanElement> planElements = new ArrayList<>();
        Schedule<?> schedule = vehicle.getSchedule();
        Id<Link> startLinkId = vehicle.getStartLink().getId();

        if (schedule.getStatus() == ScheduleStatus.UNPLANNED) {// vehicle stays on startLink until t1
            planElements
                    .add(createActivity("Unplanned", startLinkId, SIM_BEGIN_TIME, vehicle.getT1()));
            return planElements;
        }

        planElements.add(createActivity("Before schedule", startLinkId, SIM_BEGIN_TIME,
                schedule.getBeginTime()));

        for (Task t : schedule.getTasks()) {
            switch (t.getType()) {
                case DRIVE:
                    planElements.add(createLeg((DriveTask)t));
                    break;

                case STAY:
                    StayTask st = (StayTask)t;
                    planElements.add(createActivity(st.getType().name(), st.getLink().getId(),
                            st.getBeginTime(), st.getEndTime()));
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        planElements.add(
                createActivity("After schedule", Schedules.getLastLinkInSchedule(schedule).getId(),
                        schedule.getEndTime(), Double.POSITIVE_INFINITY));

        return planElements;
    }


    private Leg createLeg(DriveTask task)
    {
        VrpPath path = task.getPath();
        Leg leg = populFactory.createLeg(TransportMode.car);
        NetworkRoute route = VrpPaths.createNetworkRoute(path, populFactory.getRouteFactories());
        leg.setRoute(route);
        return leg;
    }


    private Activity createActivity(String actType, Id<Link> linkId, double startTime,
            double endTime)
    {
        Activity act = populFactory.createActivityFromLinkId(actType, linkId);
        act.setStartTime(startTime);
        act.setEndTime(endTime);
        return act;
    }


    private class VrpSchedulePlan
        implements Plan
    {
        private final Vehicle vehicle;
        private List<PlanElement> unmodifiablePlanElements;//lazily initialised


        public VrpSchedulePlan(Vehicle vehicle)
        {
            this.vehicle = vehicle;
        }


        @Override
        public List<PlanElement> getPlanElements()
        {
            if (unmodifiablePlanElements == null) {
                unmodifiablePlanElements = Collections.unmodifiableList(initPlanElements(vehicle));
            }
            return unmodifiablePlanElements;
        }


        @Override
        public Person getPerson()
        {
            throw new UnsupportedOperationException();
        }


        public Double getScore()
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public Map<String, Object> getCustomAttributes()
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public void addLeg(Leg leg)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public void addActivity(Activity act)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public String getType()
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public void setType(String type)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public void setScore(Double score)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public void setPerson(Person person)
        {
            throw new UnsupportedOperationException();
        }


        @Override
        public Attributes getAttributes()
        {
            throw new UnsupportedOperationException();
        }
    }
}
