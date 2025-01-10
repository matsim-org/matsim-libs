package org.matsim.dsim.simulation;

import com.google.inject.Inject;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkPartition;
import org.matsim.api.core.v01.network.NetworkPartitioning;
import org.matsim.core.mobsim.dsim.*;
import org.matsim.dsim.MessageBroker;

/**
 * Bridge between simulation and Message broker. Have this behind an interface, so that
 * we can mock this object in tests easily
 */
public class SimStepMessaging {

	private final NetworkPartitioning networkPartitioning;
	private final NetworkPartition partition;
	// members are final but are mutable during the simulation
	private final MessageBroker messageBroker;
	private final Int2ObjectMap<SimStepMessage.Builder> msgs = new Int2ObjectOpenHashMap<>();

	@Inject
	public SimStepMessaging(Network network, NetworkPartition partition, MessageBroker messageBroker) {
		this.networkPartitioning = network.getPartitioning();
		this.partition = partition;
		this.messageBroker = messageBroker;

		for (int neighbor : partition.getNeighbors()) {
			msgs.computeIfAbsent(neighbor, _ -> SimStepMessage.builder());
		}
	}

	public void collectTeleportation(DistributedMobsimAgent person, double exitTime) {

		// figure out where the person has to go and store the person // we are expecting teleported persons here.
		int targetPart = networkPartitioning.getPartition(person.getDestinationLinkId());
		var teleportation = new Teleportation(
			person.getClass(), person.toMessage(), exitTime
		);

		msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
			.addTeleportation(teleportation);
	}

	public void collectStorageCapacityUpdate(Id<Link> linkId, double released, double consumed, int targetPart) {
		var capacityUpdateMessage = new CapacityUpdate(
			linkId, released, consumed
		);

		msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
			.addCapacityUpdate(capacityUpdateMessage);
	}

	public void collectVehicle(DistributedMobsimVehicle simVehicle) {

		Id<Link> currentLinkId = simVehicle.getCurrentLinkId();
		int targetPart = networkPartitioning.getPartition(currentLinkId);
		msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
			.addVehicleContainer(AgentSourcesContainer.vehicleToContainer(simVehicle));
	}

	public void sendMessages(double now) {

		var it = msgs.int2ObjectEntrySet().iterator();
		while (it.hasNext()) {

			// build and send a message to the target partition
			var msgEntry = it.next();
			var msgBuilder = msgEntry.getValue();
			int targetPart = msgEntry.getIntKey();
			SimStepMessage msg = msgBuilder.setSimstep(now).build();
			messageBroker.send(msg, targetPart);

			// update the bookkeeping. Since we must send to neighbor partitions, we clear the builder and keep it
			// if we encounter a message builder for a remote partition, i.e. for teleportation, we remove it from
			// the map, as we don't know whether we will need it in the next time step.
			if (partition.getNeighbors().contains(targetPart)) {
				msgBuilder.clear();
			} else {
				it.remove();
			}
		}
	}

	public boolean isLocal(Id<Link> linkId) {
		return partition.containsLink(linkId);
	}
}
