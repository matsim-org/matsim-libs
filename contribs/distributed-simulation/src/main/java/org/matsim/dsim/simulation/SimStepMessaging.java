package org.matsim.dsim.simulation;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.framework.DistributedMobsimAgent;
import org.matsim.core.mobsim.qsim.interfaces.DistributedMobsimVehicle;
import org.matsim.dsim.MessageBroker;
import org.matsim.dsim.QSimCompatibility;
import org.matsim.dsim.messages.CapacityUpdate;
import org.matsim.dsim.messages.SimStepMessage;
import org.matsim.dsim.messages.Teleportation;

import static org.matsim.dsim.NetworkDecomposition.PARTITION_ATTR_KEY;

/**
 * Bridge between simulation and Message broker. Have this behind an interface, so that
 * we can mock this object in tests easily
 */
public interface SimStepMessaging {
	void collectTeleportation(DistributedMobsimAgent person, double exitTime);

	void collectStorageCapacityUpdate(Id<Link> linkId, double released, double consumed, int targetPart);

	void collectVehicle(DistributedMobsimVehicle simVehicle);

	void sendMessages(double time);

	boolean isLocal(Id<Link> linkId);

	int getPart();

	IntSet getNeighbors();

	static SimStepMessaging create(Network network, MessageBroker messageBroker, QSimCompatibility qsim, IntSet neighbors, int part) {
		return new SimStepMessagingImpl(network, messageBroker, qsim, neighbors, part);
	}

	@Log4j2
	class SimStepMessagingImpl implements SimStepMessaging {

		// values that don't change over the simulation
		private final Object2IntMap<Id<Link>> part2Link;
		@Getter
		private final int part;
		@Getter
		private final IntSet neighbors;

		// members are final but are mutable during the simulation
		private final MessageBroker messageBroker;
		private final QSimCompatibility qsim;
		private final Int2ObjectMap<SimStepMessage.SimStepMessageBuilder> msgs = new Int2ObjectOpenHashMap<>();

		public SimStepMessagingImpl(Network globalNetwork, MessageBroker messageBroker, QSimCompatibility qsim, IntSet neighbors, int part) {
			this.messageBroker = messageBroker;
			this.qsim = qsim;
			var link2RankMapping = new Object2IntOpenHashMap<Id<Link>>();
			for (var link : globalNetwork.getLinks().values()) {
				Id<Link> id = link.getId();
				int rank = (int) link.getAttributes().getAttribute(PARTITION_ATTR_KEY);
				link2RankMapping.put(id, rank);
			}
			this.part2Link = link2RankMapping;
			this.neighbors = neighbors;
			this.part = part;
			for (var neighbor : neighbors) {
				msgs.computeIfAbsent(neighbor, _ -> SimStepMessage.builder());
			}
		}

		public void collectTeleportation(DistributedMobsimAgent person, double exitTime) {

			// figure out where the person has to go and store the person // we are expecting teleported persons here.
			int targetPart = part2Link.getInt(person.getDestinationLinkId());
			Teleportation teleportation = Teleportation.builder()
				.setPersonMessage(person.toMessage())
				.setExitTime(exitTime)
				.build();

			msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
				.setTeleportationMsg(teleportation);
		}

		public void collectStorageCapacityUpdate(Id<Link> linkId, double released, double consumed, int targetPart) {
			var capacityUpdateMessage = CapacityUpdate.builder()
				.setLinkId(linkId)
				.setReleased(released)
				.setConsumed(consumed)
				.build();
			msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
				.setCapacityUpdate(capacityUpdateMessage);
		}

		public void collectVehicle(DistributedMobsimVehicle simVehicle) {

			Id<Link> currentLinkId = simVehicle.getCurrentLinkId();
			int targetPart = part2Link.getInt(currentLinkId);
			msgs.computeIfAbsent(targetPart, _ -> SimStepMessage.builder())
				.setVehicle(qsim.vehicleToContainer(simVehicle));
		}

		public void sendMessages(double now) {

			var it = msgs.int2ObjectEntrySet().iterator();
			while (it.hasNext()) {

				// build and send a message to the target partition
				var msgEntry = it.next();
				SimStepMessage.SimStepMessageBuilder msgBuilder = msgEntry.getValue();
				int targetPart = msgEntry.getIntKey();
				messageBroker.send(msgBuilder.setSimstep(now).build(), targetPart);

				// update the bookkeeping. Since we must send to neighbor partitions, we clear the builder and keep it
				// if we encounter a message builder for a remote partition, i.e. for teleportation, we remove it from
				// the map, as we don't know whether we will need it in the next time step.
				if (neighbors.contains(targetPart)) {
					msgBuilder.clearVehicles();
					msgBuilder.clearCapacityUpdates();
					msgBuilder.clearTeleportationMsgs();
				} else {
					it.remove();
				}
			}
		}

		public boolean isLocal(Id<Link> linkId) {
			var linkRank = part2Link.getInt(linkId);
			return linkRank == part;
		}
	}
}
