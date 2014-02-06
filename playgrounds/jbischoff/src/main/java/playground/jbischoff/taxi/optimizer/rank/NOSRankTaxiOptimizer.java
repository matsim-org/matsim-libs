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

package playground.jbischoff.taxi.optimizer.rank;

import java.util.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.MatsimVrpContext;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.*;
import org.matsim.contrib.dvrp.schedule.*;

import playground.jbischoff.energy.charging.RankArrivalDepartureCharger;
import playground.michalm.taxi.optimizer.immediaterequest.*;
import playground.michalm.taxi.schedule.*;


/**
 * @author jbischoff
 */

public class NOSRankTaxiOptimizer
    extends NOSTaxiOptimizer
{
    private MatsimVrpContext context;
    private VrpPathCalculator calculator;
    private IdleRankVehicleFinder idleVehicleFinder;

    private boolean idleRankMode;

    private final List<Id> shortTimeIdlers;

    private RankArrivalDepartureCharger rankArrivalDepartureCharger;


    public static NOSRankTaxiOptimizer createNOSRankTaxiOptimizer(MatsimVrpContext context,
            VrpPathCalculator calculator, ImmediateRequestParams params,
            boolean straightLineDistance)
    {
        return new NOSRankTaxiOptimizer(context, calculator, params, new IdleRankVehicleFinder(
                context, calculator, straightLineDistance));
    }


    private NOSRankTaxiOptimizer(MatsimVrpContext context, VrpPathCalculator calculator,
            ImmediateRequestParams params, IdleRankVehicleFinder vehicleFinder)
    {
        super(new RankModeTaxiScheduler(context, calculator, params), context, vehicleFinder, false);
        this.context = context;
        this.calculator = calculator;
        this.idleVehicleFinder = vehicleFinder;
        this.shortTimeIdlers = new ArrayList<Id>();
    }


    public void setRankMode(boolean rankMode)
    {
        ((RankModeTaxiScheduler)getScheduler()).rankmode = rankMode;
    }


    public void setRankArrivalCharger(RankArrivalDepartureCharger rankArrivalDepartureCharger)
    {
        this.rankArrivalDepartureCharger = rankArrivalDepartureCharger;
        this.idleVehicleFinder.addRankArrivalCharger(this.rankArrivalDepartureCharger);
    }


    private static class RankModeTaxiScheduler
        extends TaxiScheduler
    {
        private boolean rankmode;
        private VrpPathCalculator calculator;


        public RankModeTaxiScheduler(MatsimVrpContext context, VrpPathCalculator calculator,
                ImmediateRequestParams params)
        {
            super(context, calculator, params);
            this.calculator = calculator;
        }


        @Override
        public void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
        {
            if (rankmode) {
                TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules
                        .getLastTask(schedule);

                Link link = dropoffStayTask.getLink();
                Link startLink = schedule.getVehicle().getStartLink();

                if (link != startLink) {
                    double t5 = dropoffStayTask.getEndTime();
                    VrpPathWithTravelData path = calculator.calcPath(link, startLink, t5);
                    schedule.addTask(new TaxiCruiseDriveTask(path));

                    double t6 = path.getArrivalTime();
                    double tEnd = Math.max(t6, schedule.getVehicle().getT1());
                    schedule.addTask(new TaxiWaitStayTask(t6, tEnd, schedule.getVehicle()
                            .getStartLink()));
                }
                else {
                    super.appendWaitAfterDropoff(schedule);
                }
            }
            else {
                super.appendWaitAfterDropoff(schedule);
            }
        }
    }


    public void setIdleRankMode(boolean b)
    {
        this.idleRankMode = b;
    }


    public void doSimStep(double time)
    {
        if (time % 60. == 0.) {
            checkWaitingVehiclesBatteryState();
            if (this.idleRankMode)
                updateIdlers();
        }
        if (this.idleRankMode)
            if (time % 60. == 5.) {
                sendIdlingTaxisToRank();
            }
    }


    private void checkWaitingVehiclesBatteryState()
    {
        for (Vehicle veh : context.getVrpData().getVehicles()) {
            Task last = Schedules.getLastTask(veh.getSchedule());
            if (last instanceof TaxiWaitStayTask) {
                TaxiWaitStayTask lastw = (TaxiWaitStayTask)last;
                if (!lastw.getLink().equals(veh.getStartLink())) {
                    if (this.rankArrivalDepartureCharger.needsToReturnToRank(veh.getId())) {
                        scheduleRankReturn(veh);
                    }
                }
            }
        }
    }


    protected void scheduleRankReturn(Vehicle veh)
    {

        @SuppressWarnings("unchecked")
        Schedule<Task> sched = (Schedule<Task>)veh.getSchedule();
        double currentTime = context.getTime();
        double oldEndTime;
        TaxiWaitStayTask lastTask = (TaxiWaitStayTask)Schedules.getLastTask(sched);// only WAIT

        switch (lastTask.getStatus()) {
            case PLANNED:
                return;

            case STARTED:
                oldEndTime = lastTask.getEndTime();
                lastTask.setEndTime(currentTime);// shortening the WAIT task

                break;

            case PERFORMED:
                throw new IllegalStateException();
            default:
                throw new IllegalStateException();
        }

        Link lastLink = lastTask.getLink();

        if (veh.getStartLink() != lastLink) {// not a loop
            VrpPathWithTravelData path = calculator.calcPath(lastLink, veh.getStartLink(),
                    currentTime);
            sched.addTask(new TaxiCruiseDriveTask(path));

            double arrivalTime = path.getArrivalTime();
            sched.addTask(new TaxiWaitStayTask(arrivalTime, oldEndTime, veh.getStartLink()));
            // System.out.println("T :"+data.getTime()+" V: "+veh.getName()+" OET:"
            // +oldendtime);
        }

    }


    public void updateIdlers()
    {
        this.shortTimeIdlers.clear();
        for (Vehicle veh : context.getVrpData().getVehicles()) {
            Task last = Schedules.getLastTask(veh.getSchedule());
            if (last instanceof TaxiWaitStayTask) {
                TaxiWaitStayTask lastw = (TaxiWaitStayTask)last;
                if (!lastw.getLink().equals(veh.getStartLink())) {
                    this.shortTimeIdlers.add(veh.getId());
                }
            }
        }
    }


    public void sendIdlingTaxisToRank()
    {
        for (Vehicle veh : context.getVrpData().getVehicles()) {
            if (shortTimeIdlers.contains(veh.getId())) {
                Task last = Schedules.getLastTask(veh.getSchedule());
                if (last instanceof TaxiWaitStayTask) {
                    scheduleRankReturn(veh);
                }
            }
        }
    }
}
