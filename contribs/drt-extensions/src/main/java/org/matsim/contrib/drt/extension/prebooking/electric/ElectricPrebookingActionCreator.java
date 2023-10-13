package org.matsim.contrib.drt.extension.prebooking.electric;

import org.matsim.contrib.drt.extension.prebooking.dvrp.PassengerEnteringVehicleHandler;
import org.matsim.contrib.drt.extension.prebooking.dvrp.PrebookingPassengerEngine;
import org.matsim.contrib.drt.extension.prebooking.dvrp.PrebookingStopActivity;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.vrpagent.VrpAgentLogic;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.dynagent.DynAgent;
import org.matsim.contrib.evrp.EvDvrpVehicle;
import org.matsim.contrib.evrp.tracker.OfflineETaskTracker;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * See PrebookingActionCreator, only here we also initialize the energy tracking
 * for the replaced stop activities.
 * 
 * @author Sebastian Hörl (sebhoerl), IRT SystemX
 */
public class ElectricPrebookingActionCreator implements VrpAgentLogic.DynActionCreator {
	private final VrpAgentLogic.DynActionCreator delegate;
	private final PrebookingPassengerEngine passengerEngine;
	private final PassengerStopDurationProvider stopDurationProvider;
	private final MobsimTimer timer;
	private final PassengerEnteringVehicleHandler enteringHandler;

	public ElectricPrebookingActionCreator(PrebookingPassengerEngine passengerEngine,
			VrpAgentLogic.DynActionCreator delegate, PassengerStopDurationProvider stopDurationProvider,
			MobsimTimer timer, PassengerEnteringVehicleHandler enteringHandler) {
		this.delegate = delegate;
		this.passengerEngine = passengerEngine;
		this.stopDurationProvider = stopDurationProvider;
		this.timer = timer;
		this.enteringHandler = enteringHandler;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, DvrpVehicle vehicle, double now) {
		Task task = vehicle.getSchedule().getCurrentTask();

		if (DrtTaskBaseType.STOP.isBaseTypeOf(task)) {
			DrtStopTask stopTask = (DrtStopTask) task;

			// added for electric
			stopTask.initTaskTracker(new OfflineETaskTracker((EvDvrpVehicle) vehicle, timer));

			return new PrebookingStopActivity(passengerEngine, dynAgent, stopTask, stopTask.getDropoffRequests(),
					stopTask.getPickupRequests(), DrtActionCreator.DRT_STOP_NAME, stopDurationProvider, vehicle,
					enteringHandler);
		}

		return delegate.createAction(dynAgent, vehicle, now);
	}

}