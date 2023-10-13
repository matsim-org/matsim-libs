package org.matsim.contrib.drt.extension.prebooking.dvrp;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;

/**
 * For prebooking, we implement an alternative DynActivity that handles entering
 * / exiting passengers more flexibly than the standard DrtStopActivity.
 * 
 * Specifically, we track when a person is ready for departure and then add the
 * expected duration of the interaction into a queue. The agent only actually
 * enters the vehicle after this time has elapsed.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class PrebookingActionCreator implements VrpAgentLogic.DynActionCreator {
	private final VrpAgentLogic.DynActionCreator delegate;
	private final PrebookingPassengerEngine passengerEngine;
	private final PassengerStopDurationProvider stopDurationProvider;
	private final EnteringHandler enteringHandler;

	public PrebookingActionCreator(PrebookingPassengerEngine passengerEngine, VrpAgentLogic.DynActionCreator delegate,
			PassengerStopDurationProvider stopDurationProvider, EnteringHandler enteringHandler) {
		this.delegate = delegate;
		this.passengerEngine = passengerEngine;
		this.stopDurationProvider = stopDurationProvider;
		this.enteringHandler = enteringHandler;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();

		if (getBaseTypeOrElseThrow(task).equals(DrtTaskBaseType.STOP)) {
			DrtStopTask stopTask = (DrtStopTask) task;
			return new PrebookingStopActivity(passengerEngine, dynAgent, stopTask, stopTask.getDropoffRequests(),
					stopTask.getPickupRequests(), DrtActionCreator.DRT_STOP_NAME, stopDurationProvider, vehicle,
					enteringHandler);
		}

		return delegate.createAction(dynAgent, vehicle, now);
	}
}
