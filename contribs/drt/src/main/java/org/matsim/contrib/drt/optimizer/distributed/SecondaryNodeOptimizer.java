package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.api.core.v01.Message;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.schedule.ScheduleTimingUpdater;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.dsim.MessageBroker;

/**
 * Optimizer that sends request to the head node and receives the optimized plan.
 */
public class SecondaryNodeOptimizer implements DrtOptimizer {

	private final DrtNodeCommunicator comm;
	private final MessageBroker broker;
	private final Fleet fleet;
	private final ScheduleTimingUpdater scheduleTimingUpdater;

	public SecondaryNodeOptimizer(DrtNodeCommunicator comm, MessageBroker broker,
								  Fleet fleet, ScheduleTimingUpdater scheduleTimingUpdater) {
		this.comm = comm;
		this.broker = broker;
		this.fleet = fleet;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
	}

	@Override
	public void requestSubmitted(Request request) {

		if (!(request instanceof DrtRequest drt)) {
			throw new IllegalArgumentException("Request must be of type DrtRequest");
		}

		broker.sendToNode(toMessage(drt), 0);
	}

	@Override
	public void nextTask(DvrpVehicle vehicle) {
		scheduleTimingUpdater.updateBeforeNextTask(vehicle);
		vehicle.getSchedule().nextTask();
	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		// TODO: receive schedules

		for (DvrpVehicle v : fleet.getVehicles().values()) {
			scheduleTimingUpdater.updateTimings(v);
		}
	}

	private Message toMessage(DrtRequest request) {
		return new RequestMessage(
			request.getId(),
			request.getSubmissionTime(),
			request.getEarliestStartTime(),
			request.getLatestStartTime(),
			request.getLatestArrivalTime(),
			request.getMaxRideDuration(),
			request.getPassengerIds(),
			request.getMode(),
			request.getFromLink().getId(),
			request.getToLink().getId()
		);
	}

}
