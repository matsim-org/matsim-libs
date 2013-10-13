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

import pl.poznan.put.vrp.dynamic.data.VrpData;
import pl.poznan.put.vrp.dynamic.data.model.*;
import pl.poznan.put.vrp.dynamic.data.network.*;
import pl.poznan.put.vrp.dynamic.data.schedule.*;
import pl.poznan.put.vrp.dynamic.data.schedule.Schedule.ScheduleStatus;
import playground.jbischoff.energy.charging.DepotArrivalDepartureCharger;
import playground.jbischoff.taxi.rank.BackToRankTask;
import playground.michalm.taxi.schedule.*;

/**
 * 
 * 
 * 
 * @author jbischoff
 * 
 */

public class NOSRankTaxiOptimizer extends RankTaxiOptimizer {
	private IdleRankVehicleFinder idleVehicleFinder;
	private boolean rankmode;

	public NOSRankTaxiOptimizer(VrpData data, boolean destinationKnown,
			boolean straightLineDistance) {
		super(data, destinationKnown);
		idleVehicleFinder = new IdleRankVehicleFinder(data,
				straightLineDistance);

	}

	public void setRankMode(boolean rankMode) {
		this.rankmode = rankMode;
	}

	public void addDepotArrivalCharger(	DepotArrivalDepartureCharger depotArrivalDepartureCharger) {
		super.addDepotArrivalCharger(depotArrivalDepartureCharger);
		this.idleVehicleFinder.addDepotArrivalCharger(this.depotArrivalDepartureCharger);
	}

	@Override
	protected VehicleDrive findBestVehicle(Request req, List<Vehicle> vehicles) {

		Vehicle veh = idleVehicleFinder.findClosestVehicle(req);

		if (veh == null) {
			// System.out.println("No car found");
			return VehicleDrive.NO_VEHICLE_DRIVE_FOUND;
		}

		return super.findBestVehicle(req, Arrays.asList(veh));
	}

	@Override
	protected boolean shouldOptimizeBeforeNextTask(Schedule<TaxiTask> schedule,
			boolean scheduleUpdated) {
		return false;
	}

	@Override
    protected boolean shouldOptimizeAfterNextTask(Schedule<TaxiTask> schedule, boolean scheduleUpdated)
    {
        if (schedule.getStatus() == ScheduleStatus.COMPLETED) {
            return false;
        }

        if (unplannedRequestQueue.isEmpty()) {
            return false;
        }

        TaxiTask tt = (TaxiTask)schedule.getCurrentTask();
        switch (tt.getTaxiTaskType()) {
            case WAIT_STAY:
            case CRUISE_DRIVE:////////????????
                return true;

            default:
                return false;
        }
    }

	@Override
	protected void appendDeliveryAndWaitTasksAfterServeTask(Schedule<TaxiTask> schedule) {
	    TaxiPickupStayTask serveTask = (TaxiPickupStayTask) Schedules.getLastTask(schedule);

		// add DELIVERY after SERVE
		Request req = ((TaxiPickupStayTask) serveTask).getRequest();
		Vertex reqFromVertex = req.getFromVertex();
		Vertex reqToVertex = req.getToVertex();
		int t3 = serveTask.getEndTime();

		if (reqFromVertex == reqToVertex) {
			// Delivery cannot be skipped otherwise the passenger will never
			// exit the taxi
			throw new IllegalStateException("Unsupported!!!!!!");
		}

		Arc arc = data.getVrpGraph().getArc(reqFromVertex, reqToVertex);
		int startIdling = t3 + arc.getTimeOnDeparture(t3);
		schedule.addTask(new TaxiPickupDriveTask(t3, startIdling, arc, req));
		// addWaitTime at the end (even 0-second WAIT)
		int tEnd = Math.max(startIdling, Schedules.getActualT1(schedule));
		int startWaiting = startIdling;
		if (this.rankmode) {

			if (reqToVertex != schedule.getVehicle().getDepot().getVertex()) {
				Arc darc = data.getVrpGraph().getArc(reqToVertex,
						schedule.getVehicle().getDepot().getVertex());
				startWaiting = startIdling
						+ darc.getTimeOnDeparture(startIdling);
				schedule.addTask(new BackToRankTask(startIdling, startWaiting,
						darc));
				schedule.addTask(new TaxiWaitStayTask(startWaiting, tEnd, schedule
						.getVehicle().getDepot().getVertex()));

			} else {
				schedule.addTask(new TaxiWaitStayTask(startIdling, tEnd,
						reqToVertex));
			}
		}

		else {
			schedule.addTask(new TaxiWaitStayTask(startIdling, tEnd, reqToVertex));
		}
	}

}
