/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package peoplemover.stop;

import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.passenger.PassengerEngine;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.tracker.OnlineTrackerListener;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class CustomizedDrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";

	private final PassengerEngine passengerEngine;
	private final VrpLegFactory legFactory;
	private final BusStopDurationCalculator busStopDurationCalculator;

	public CustomizedDrtActionCreator(PassengerEngine passengerEngine, MobsimTimer timer, DvrpConfigGroup dvrpCfg,
			BusStopDurationCalculator busStopDurationCalculator) {
		this.passengerEngine = passengerEngine;
		this.legFactory = v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v,
				OnlineTrackerListener.NO_LISTENER, timer);
		this.busStopDurationCalculator = busStopDurationCalculator;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		DrtTask task = (DrtTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getDrtTaskType()) {
			case DRIVE:
				return legFactory.create(vehicle);

			case STOP:
				DrtStopTask t = (DrtStopTask)task;
				double duration = busStopDurationCalculator.calcDuration(t);
				return new VariableDurationBusStopActivity(passengerEngine, dynAgent, t, duration, DRT_STOP_NAME);

			case STAY:
				return new VrpActivity(DRT_STAY_NAME, (DrtStayTask)task);

			default:
				throw new IllegalStateException();
		}
	}
}
