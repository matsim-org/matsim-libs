package org.matsim.contrib.drt.optimizer.distributed;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.core.communication.Communicator;
import org.matsim.core.mobsim.dsim.NodeSingleton;
import org.matsim.core.mobsim.framework.events.MobsimAfterSimStepEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimAfterSimStepListener;
import org.matsim.dsim.MessageBroker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class running on the head node to receive requests and send out schedules.
 */
@NodeSingleton
public class DrtNodeCommunicator implements MobsimAfterSimStepListener {

	private final Network network;
	private final MessageBroker broker;

	private final Map<String, List<DrtRequest>> requests = new ConcurrentHashMap<>();
	private final Map<Id<DvrpVehicle>, Schedule> schedules = new ConcurrentHashMap<>();

	@Inject
	public DrtNodeCommunicator(Network network, MessageBroker broker) {
		this.network = network;
		this.broker = broker;
	}

	@Override
	public void notifyMobsimAfterSimStep(MobsimAfterSimStepEvent e) {
		broker.receiveNodeMessages(RequestMessage.class, this::handleMessage);
		broker.receiveNodeMessages(ScheduleMessage.class, this::handleSchedule);
	}

	/**
	 * Retrieve the requests received for a mode. This will clear the requests for that mode.
	 */
	public List<DrtRequest> getRequests(String mode) {
		return requests.containsKey(mode) ? requests.remove(mode) : List.of();
	}

	/**
	 * Return schedule of a vehicle.
	 */
	public Schedule getSchedule(Id<DvrpVehicle> vehicleId) {
		return schedules.get(vehicleId);
	}

	private void handleMessage(RequestMessage message) {
		requests.computeIfAbsent(message.mode(), k -> new ArrayList<>()).add(DrtRequest.newBuilder()
			.mode(message.mode())
			.id(message.id())
			.submissionTime(message.submissionTime())
			.constraints(message.constraints())
			.fromLink(network.getLinks().get(Id.createLinkId(message.fromLink())))
			.toLink(network.getLinks().get(Id.createLinkId(message.toLink())))
			.passengerIds(message.passengerIds())
			.build()
		);
	}

	private void handleSchedule(ScheduleMessage scheduleMessage) {
		schedules.putAll(scheduleMessage.getSchedules());
	}

	public void sendSchedules(Fleet fleet) {
		ScheduleMessage message = new ScheduleMessage();

		for (DvrpVehicle v : fleet.getVehicles().values()) {
			message.addSchedule(v.getId(), v.getSchedule());
		}

		broker.sendToNode(message, Communicator.BROADCAST_TO_ALL);
	}
}
