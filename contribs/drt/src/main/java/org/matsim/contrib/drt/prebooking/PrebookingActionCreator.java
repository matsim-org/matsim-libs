package org.matsim.contrib.drt.prebooking;

import static org.matsim.contrib.drt.schedule.DrtTaskBaseType.getBaseTypeOrElseThrow;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Verify;
import org.matsim.contrib.drt.passenger.AcceptedDrtRequest;
import org.matsim.contrib.drt.prebooking.abandon.AbandonVoter;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.stops.PassengerStopDurationProvider;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.dvrp_load.DvrpLoad;
import org.matsim.contrib.dvrp.passenger.PassengerHandler;
import org.matsim.contrib.dvrp.passenger.VehicleCapacityChangeActivity;
import org.matsim.contrib.dvrp.schedule.CapacityChangeTask;
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
			if(task instanceof CapacityChangeTask capacityChangeTask) {
				return new VehicleCapacityChangeActivity(DrtActionCreator.DRT_CAPACITY_CHANGE_NAME, vehicle, capacityChangeTask.getNewVehicleCapacity(), task.getEndTime());
			}
			DrtStopTask stopTask = (DrtStopTask) task;
			DvrpLoad incomingCapacity = getIncomingOccupancy(vehicle, stopTask);

			return new PrebookingStopActivity(passengerHandler, dynAgent, stopTask, stopTask.getDropoffRequests(),
					stopTask.getPickupRequests(), DrtActionCreator.DRT_STOP_NAME, () -> stopTask.getEndTime(),
					stopDurationProvider, vehicle, prebookingManager, abandonVoter, incomingCapacity);
		}

		return delegate.createAction(dynAgent, vehicle, now);
	}

	private DvrpLoad getIncomingOccupancy(DvrpVehicle vehicle, DrtStopTask referenceTask) {
		DvrpLoad incomingOccupancy = vehicle.getCapacity().getType().getEmptyLoad();
		List<DvrpLoad> vehicleCapacities = new ArrayList<>();

		List<? extends Task> tasks = vehicle.getSchedule().getTasks();
		vehicleCapacities.add(vehicle.getCapacity());
		boolean encounteredCurrentTask = false;
		for(Task task : tasks) {
			if(task == vehicle.getSchedule().getCurrentTask()) {
				encounteredCurrentTask = true;
			}
			if(encounteredCurrentTask && task instanceof CapacityChangeTask capacityChangeTask) {
				vehicleCapacities.add(capacityChangeTask.getNewVehicleCapacity());
			}
		}

		int currentCapacityIndex = vehicleCapacities.size() - 1;
		incomingOccupancy = vehicleCapacities.get(currentCapacityIndex).getType().getEmptyLoad();

		int index = tasks.size() - 1;
		while (index >= 0) {
			Task task = tasks.get(index);

			if(task instanceof CapacityChangeTask) {
				Verify.verify(incomingOccupancy.isEmpty());
				currentCapacityIndex--;
				incomingOccupancy = vehicleCapacities.get(currentCapacityIndex).getType().getEmptyLoad();
			} else if (task instanceof DrtStopTask) {
				DrtStopTask stopTask = (DrtStopTask) task;

				incomingOccupancy = incomingOccupancy.add(stopTask.getDropoffRequests().values().stream()
					.map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(incomingOccupancy.getType().getEmptyLoad()));

				incomingOccupancy = incomingOccupancy.subtract(stopTask.getPickupRequests().values().stream()
					.map(AcceptedDrtRequest::getLoad).reduce(DvrpLoad::add).orElse(incomingOccupancy.getType().getEmptyLoad()));

				if (task == referenceTask) {
					return incomingOccupancy;
				}
			}

			index--;
		}

		throw new IllegalStateException();
	}
}
