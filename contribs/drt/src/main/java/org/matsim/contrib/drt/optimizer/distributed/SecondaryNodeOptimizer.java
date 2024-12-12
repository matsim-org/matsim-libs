package org.matsim.contrib.drt.optimizer.distributed;

import org.matsim.api.core.v01.Message;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.mobsim.framework.events.MobsimBeforeSimStepEvent;
import org.matsim.dsim.MessageBroker;

/**
 * Optimizer that sends request to the head node and receives the optimized plan.
 */
public class SecondaryNodeOptimizer implements DrtOptimizer {

	private final MessageBroker broker;

	public SecondaryNodeOptimizer(MessageBroker broker) {
		this.broker = broker;
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

	}

	@Override
	public void notifyMobsimBeforeSimStep(MobsimBeforeSimStepEvent e) {

		// use the received schedules

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
