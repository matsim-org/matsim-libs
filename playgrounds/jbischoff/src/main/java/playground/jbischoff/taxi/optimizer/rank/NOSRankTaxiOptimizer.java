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

import org.matsim.core.basic.v01.IdImpl;

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.Vehicle;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
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

    private final List<Integer> shortTimeIdlers;

    private DepotArrivalDepartureCharger depotArrivalDepartureCharger;


    public static NOSRankTaxiOptimizer createNOSRankTaxiOptimizer(VrpData data,
            boolean destinationKnown, boolean minimizePickupTripTime, int pickupDuration,
            boolean straightLineDistance)
    {
        return new NOSRankTaxiOptimizer(data, destinationKnown, minimizePickupTripTime,
                pickupDuration, new IdleRankVehicleFinder(data, straightLineDistance));
    }


    private NOSRankTaxiOptimizer(VrpData data, boolean destinationKnown,
            boolean minimizePickupTripTime, int pickupDuration, IdleRankVehicleFinder vehicleFinder)
    {
        super(data, destinationKnown, minimizePickupTripTime, pickupDuration, vehicleFinder);
        this.idleVehicleFinder = vehicleFinder;
        this.shortTimeIdlers = new ArrayList<Integer>();
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
            TaxiDropoffStayTask dropoffStayTask = (TaxiDropoffStayTask)Schedules.getLastTask(schedule);

            Vertex vertex = dropoffStayTask.getVertex();
            Vertex depotVertex = schedule.getVehicle().getDepot().getVertex();

            if (vertex != depotVertex) {
                int t5 = dropoffStayTask.getEndTime();
                Arc arc = data.getVrpGraph().getArc(vertex, depotVertex);
                int t6 = arc.getTimeOnDeparture(t5);
                schedule.addTask(new TaxiCruiseDriveTask(t5, t6, arc));

                int tEnd = Math.max(t6, Schedules.getActualT1(schedule));
                schedule.addTask(new TaxiWaitStayTask(t6, tEnd, schedule.getVehicle()
                        .getDepot().getVertex()));
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
                if (!lastw.getVertex().equals(veh.getDepot().getVertex())) {
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

        Vertex lastVertex = lastTask.getVertex();

        if (veh.getDepot().getVertex() != lastVertex) {// not a loop
            Arc darc = data.getVrpGraph().getArc(lastVertex, veh.getDepot().getVertex());
            int arrivalTime = darc.getTimeOnDeparture(currentTime) + currentTime;

            sched.addTask(new TaxiCruiseDriveTask(currentTime, arrivalTime, darc));
            sched.addTask(new TaxiWaitStayTask(arrivalTime, oldendtime, veh.getDepot().getVertex()));
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
                if (!lastw.getVertex().equals(veh.getDepot().getVertex())) {
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
