package org.matsim.contrib.drt.prebooking;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

import org.matsim.contrib.drt.prebooking.abandon.AbandonVoter;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
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
	private final PassengerHandler passengerHandler;
	private final PassengerStopDurationProvider stopDurationProvider;
	private final PrebookingManager prebookingManager;
	private final AbandonVoter abandonVoter;

	public PrebookingActionCreator(PassengerHandler passengerHandler, VrpAgentLogic.DynActionCreator delegate,
			PassengerStopDurationProvider stopDurationProvider, PrebookingManager prebookingManager,
			AbandonVoter abandonVoter) {
		this.delegate = delegate;
		this.passengerHandler = passengerHandler;
		this.stopDurationProvider = stopDurationProvider;
		this.prebookingManager = prebookingManager;
		this.abandonVoter = abandonVoter;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();

		if (getBaseTypeOrElseThrow(task).equals(DrtTaskBaseType.STOP)) {
			DrtStopTask stopTask = (DrtStopTask) task;
			return new PrebookingStopActivity(passengerHandler, dynAgent, stopTask, stopTask.getDropoffRequests(),
					stopTask.getPickupRequests(), DrtActionCreator.DRT_STOP_NAME, () -> stopTask.getEndTime(),
					stopDurationProvider, vehicle, prebookingManager, abandonVoter);
		}

		return delegate.createAction(dynAgent, vehicle, now);
	}
}
