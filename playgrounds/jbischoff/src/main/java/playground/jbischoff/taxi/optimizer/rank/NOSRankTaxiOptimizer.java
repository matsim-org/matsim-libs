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
import org.matsim.contrib.dvrp.data.VrpData;
import org.matsim.contrib.dvrp.data.model.Vehicle;
import org.matsim.contrib.dvrp.data.network.*;
import org.matsim.contrib.dvrp.data.schedule.*;
import org.matsim.core.basic.v01.IdImpl;

import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.michalm.taxi.optimizer.immediaterequest.NOSTaxiOptimizer;
import playground.michalm.taxi.schedule.*;


/**
 * @author jbischoff
 */

public class NOSRankTaxiOptimizer
    extends NOSTaxiOptimizer
{
    private IdleRankVehicleFinder idleVehicleFinder;

    private boolean idleRankMode;
    private boolean rankmode;

    private final List<Id> shortTimeIdlers;

    private DepotArrivalDepartureCharger depotArrivalDepartureCharger;
    private final VrpPathCalculator calculator;


    public static NOSRankTaxiOptimizer createNOSRankTaxiOptimizer(VrpData data,
            boolean destinationKnown, boolean minimizePickupTripTime, int pickupDuration,
            int dropoffDuration, boolean straightLineDistance)
    {
        return new NOSRankTaxiOptimizer(data, destinationKnown, minimizePickupTripTime,
                pickupDuration, dropoffDuration, new IdleRankVehicleFinder(data,
                        straightLineDistance));
    }


    private NOSRankTaxiOptimizer(VrpData data, boolean destinationKnown,
            boolean minimizePickupTripTime, int pickupDuration, int dropoffDuration,
            IdleRankVehicleFinder vehicleFinder)
    {
        super(data, destinationKnown, minimizePickupTripTime, pickupDuration, dropoffDuration,
                vehicleFinder);
        this.idleVehicleFinder = vehicleFinder;
        this.shortTimeIdlers = new ArrayList<Id>();
        this.calculator = data.getPathCalculator();
    }


    public void setRankMode(boolean rankMode)
    {
        this.rankmode = rankMode;
    }


    public void setDepotArrivalCharger(DepotArrivalDepartureCharger depotArrivalDepartureCharger)
    {
        this.depotArrivalDepartureCharger = depotArrivalDepartureCharger;
        this.idleVehicleFinder.addDepotArrivalCharger(this.depotArrivalDepartureCharger);
    }


    @Override
    protected void appendWaitAfterDropoff(Schedule<TaxiTask> schedule)
    {
        if (this.rankmode) {
            TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules
                    .getLastTask(schedule);

            Link link = dropoffStayTask.getLink();
            Link depotLink = schedule.getVehicle().getDepot().getLink();

            if (link != depotLink) {
                int t5 = dropoffStayTask.getEndTime();
                VrpPath path = calculator.calcPath(link, depotLink, t5);
                schedule.addTask(new TaxiCruiseDriveTask(path));

                int t6 = path.getArrivalTime();
                int tEnd = Math.max(t6, Schedules.getActualT1(schedule));
                schedule.addTask(new TaxiWaitStayTask(t6, tEnd, schedule.getVehicle().getDepot()
                        .getLink()));
            }
            else {
                super.appendWaitAfterDropoff(schedule);
            }
        }
        else {
            super.appendWaitAfterDropoff(schedule);
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
        for (Vehicle veh : data.getVehicles()) {
            Task last = Schedules.getLastTask(veh.getSchedule());
            if (last instanceof TaxiWaitStayTask) {
                TaxiWaitStayTask lastw = (TaxiWaitStayTask)last;
                if (!lastw.getLink().equals(veh.getDepot().getLink())) {
                    if (this.depotArrivalDepartureCharger.needsToReturnToRank(new IdImpl(veh
                            .getName()))) {
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
        int currentTime = data.getTime();
        int oldendtime;
        TaxiWaitStayTask lastTask = (TaxiWaitStayTask)Schedules.getLastTask(sched);// only WAIT

        switch (lastTask.getStatus()) {
            case PLANNED:
                return;

            case STARTED:
                oldendtime = lastTask.getEndTime();
                lastTask.setEndTime(currentTime);// shortening the WAIT task

                break;

            case PERFORMED:
                throw new IllegalStateException();
            default:
                throw new IllegalStateException();
        }

        Link lastLink = lastTask.getLink();

        if (veh.getDepot().getLink() != lastLink) {// not a loop
            VrpPath path = calculator.calcPath(lastLink, veh.getDepot().getLink(),
                    currentTime);
            sched.addTask(new TaxiCruiseDriveTask(path));

            int arrivalTime = path.getArrivalTime();
            sched.addTask(new TaxiWaitStayTask(arrivalTime, oldendtime, veh.getDepot().getLink()));
            // System.out.println("T :"+data.getTime()+" V: "+veh.getName()+" OET:"
            // +oldendtime);
        }

    }


    public void updateIdlers()
    {
        this.shortTimeIdlers.clear();
        for (Vehicle veh : data.getVehicles()) {
            Task last = Schedules.getLastTask(veh.getSchedule());
            if (last instanceof TaxiWaitStayTask) {
                TaxiWaitStayTask lastw = (TaxiWaitStayTask)last;
                if (!lastw.getLink().equals(veh.getDepot().getLink())) {
                    this.shortTimeIdlers.add(veh.getId());
                }
            }
        }
    }


    public void sendIdlingTaxisToRank()
    {
        for (Vehicle veh : data.getVehicles()) {
            if (shortTimeIdlers.contains(veh.getId())) {
                Task last = Schedules.getLastTask(veh.getSchedule());
                if (last instanceof TaxiWaitStayTask) {
                    scheduleRankReturn(veh);
                }
            }
        }
    }
}
